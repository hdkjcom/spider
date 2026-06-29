package io.github.spider.grpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import io.github.spider.core.transport.SpiderTransport;
import io.grpc.*;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Spider 通用 gRPC 传输层。
 *
 * 将 SpiderRequest (JSON) 转换为 gRPC DynamicMessage，执行一元调用，再将 DynamicMessage 转换回 JSON 响应。
 */
public class GrpcSpiderTransport implements SpiderTransport {

    private final ManagedChannel channel;
    private final Map<String, GrpcMethodBinding> bindings = new ConcurrentHashMap<>();

    public GrpcSpiderTransport(ManagedChannel channel) {
        this.channel = channel;
    }

    /**
     * 注册一个 gRPC 方法绑定。
     *
     * @param spiderPath   Spider 路径（例如 "/greet"）
     * @param serviceName  gRPC 服务名（例如 "greet.Greeter"）
     * @param methodName   gRPC 方法名（例如 "SayHello"）
     * @param methodDesc   gRPC MethodDescriptor 描述符
     * @param requestDesc  请求消息的 Protobuf 描述符
     * @param responseDesc 响应消息的 Protobuf 描述符
     */
    public void registerMethod(String spiderPath, String serviceName, String methodName,
                                MethodDescriptor<DynamicMessage, DynamicMessage> methodDesc,
                                Descriptors.Descriptor requestDesc,
                                Descriptors.Descriptor responseDesc) {
        String fullMethodName = MethodDescriptor.generateFullMethodName(serviceName, methodName);
        bindings.put(spiderPath, new GrpcMethodBinding(fullMethodName, methodDesc, requestDesc, responseDesc));
    }

    @Override
    public SpiderResponse execute(SpiderRequest request) throws IOException {
        GrpcMethodBinding binding = findBinding(request.path());
        if (binding == null) {
            throw new IOException("No gRPC method binding found for path: " + request.path());
        }

        long start = System.currentTimeMillis();

        // 从 JSON 请求体构建 DynamicMessage
        DynamicMessage requestMessage = buildRequest(binding, request.body());

        // 执行一元 gRPC 调用
        ClientCall<DynamicMessage, DynamicMessage> call =
                channel.newCall(binding.methodDescriptor, CallOptions.DEFAULT);
        DynamicMessage responseMessage;
        try {
            responseMessage = ClientCalls.blockingUnaryCall(call, requestMessage);
        } catch (StatusRuntimeException e) {
            throw new IOException("gRPC call failed: " + e.getStatus(), e);
        }

        long elapsed = System.currentTimeMillis() - start;

        // 将响应转换为 JSON
        byte[] responseBody = new byte[0];
        if (responseMessage != null) {
            try {
                String json = JsonFormat.printer().print(responseMessage);
                responseBody = json.getBytes(StandardCharsets.UTF_8);
            } catch (InvalidProtocolBufferException e) {
                throw new IOException("Failed to serialize gRPC response to JSON", e);
            }
        }

        return new SpiderResponse()
                .statusCode(200)
                .bodyBytes(responseBody)
                .elapsedMillis(elapsed);
    }

    private DynamicMessage buildRequest(GrpcMethodBinding binding, byte[] jsonBody) throws IOException {
        if (jsonBody == null || jsonBody.length == 0) {
            return DynamicMessage.getDefaultInstance(binding.requestDescriptor);
        }
        try {
            String json = new String(jsonBody, StandardCharsets.UTF_8);
            DynamicMessage.Builder builder = DynamicMessage.newBuilder(binding.requestDescriptor);
            JsonFormat.parser().merge(json, builder);
            return builder.build();
        } catch (InvalidProtocolBufferException e) {
            throw new IOException("Failed to parse request JSON to protobuf", e);
        }
    }

    private GrpcMethodBinding findBinding(String path) {
        GrpcMethodBinding binding = bindings.get(path);
        if (binding != null) return binding;

        String bestMatch = null;
        for (String key : bindings.keySet()) {
            if (path.startsWith(key) && (bestMatch == null || key.length() > bestMatch.length())) {
                bestMatch = key;
            }
        }
        return bestMatch != null ? bindings.get(bestMatch) : null;
    }

    /** 关闭底层 Channel。 */
    public void shutdown() throws InterruptedException {
        if (!channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)) {
            channel.shutdownNow();
        }
    }

    // ---- 流式调用支持 ----

    /**
     * 执行服务端流式 gRPC 调用，返回响应的迭代器。
     * 每个响应是 JSON 编码的 DynamicMessage。
     */
    public Iterator<String> executeStreaming(SpiderRequest request) throws IOException {
        GrpcMethodBinding binding = findBinding(request.path());
        if (binding == null) {
            throw new IOException("No gRPC method binding found for path: " + request.path());
        }

        DynamicMessage requestMessage = buildRequest(binding, request.body());
        final ClientCall<DynamicMessage, DynamicMessage> call =
                channel.newCall(binding.methodDescriptor, CallOptions.DEFAULT);

        BlockingQueue<StreamItem> responseQueue = new ArrayBlockingQueue<>(256);

        ClientCalls.asyncServerStreamingCall(call, requestMessage,
                new StreamObserver<DynamicMessage>() {
                    @Override
                    public void onNext(DynamicMessage value) {
                        try {
                            String json = JsonFormat.printer().print(value);
                            responseQueue.add(new StreamItem(json, null, false));
                        } catch (Exception e) {
                            onError(e);
                        }
                    }
                    @Override
                    public void onError(Throwable t) {
                        responseQueue.add(new StreamItem(null, t.getMessage(), false));
                    }
                    @Override
                    public void onCompleted() {
                        responseQueue.add(new StreamItem(null, null, true));
                    }
                });

        return new Iterator<String>() {
            private String next = null;
            private boolean done = false;

            @Override
            public boolean hasNext() {
                if (done) return false;
                if (next != null) return true;
                try {
                    StreamItem item = responseQueue.poll(30, TimeUnit.SECONDS);
                    if (item == null) return false; // 超时
                    if (item.done) { done = true; call.cancel(null, null); return false; }
                    if (item.error != null) {
                        call.cancel(item.error, null);
                        throw new RuntimeException(item.error);
                    }
                    next = item.json;
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            @Override
            public String next() {
                if (!hasNext()) throw new IllegalStateException("No more elements");
                String result = next;
                next = null;
                return result;
            }
        };
    }

    // ---- 内部类型 ----

    private static class StreamItem {
        final String json;
        final String error;
        final boolean done;

        StreamItem(String json, String error, boolean done) {
            this.json = json;
            this.error = error;
            this.done = done;
        }
    }

    private static class GrpcMethodBinding {
        final String fullMethodName;
        final MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor;
        final Descriptors.Descriptor requestDescriptor;
        final Descriptors.Descriptor responseDescriptor;

        GrpcMethodBinding(String fullMethodName, MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor,
                          Descriptors.Descriptor requestDescriptor, Descriptors.Descriptor responseDescriptor) {
            this.fullMethodName = fullMethodName;
            this.methodDescriptor = methodDescriptor;
            this.requestDescriptor = requestDescriptor;
            this.responseDescriptor = responseDescriptor;
        }
    }
}
