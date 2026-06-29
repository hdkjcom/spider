package io.github.spider.http;

import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import io.github.spider.core.transport.SpiderTransport;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于 OkHttp 的 SpiderTransport 实现。
 * 将 SpiderRequest 转换为 OkHttp 请求并执行，再将响应转为 SpiderResponse。
 *
 * <p>支持两种超时模式：
 * <ul>
 *   <li>如果 {@link SpiderRequest#timeoutMillis()} &gt; 0，则为该请求创建带有自定义超时的临时 client</li>
 *   <li>否则使用默认的 OkHttpClient 实例</li>
 * </ul>
 */
public class OkHttpSpiderTransport implements SpiderTransport {

    private final OkHttpClient httpClient;

    /**
     * 使用默认超时配置创建 OkHttpSpiderTransport 实例。
     * 连接超时 10 秒，读/写超时各 30 秒。
     */
    public OkHttpSpiderTransport() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 使用自定义 OkHttpClient 实例创建 OkHttpSpiderTransport。
     *
     * @param httpClient 自定义的 OkHttpClient 实例
     */
    public OkHttpSpiderTransport(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * 执行 SpiderRequest，将其转换为 OkHttp 请求发送，并将响应包装为 SpiderResponse。
     *
     * @param request Spider 请求对象
     * @return SpiderResponse 响应对象
     * @throws IOException 网络 I/O 异常时抛出
     */
    @Override
    public SpiderResponse execute(SpiderRequest request) throws IOException {
        long start = System.currentTimeMillis();

        // 如果请求指定了超时，为该请求创建带自定义超时的临时 client
        OkHttpClient client = this.httpClient;
        int requestTimeout = request.timeoutMillis();
        if (requestTimeout > 0) {
            client = this.httpClient.newBuilder()
                    .callTimeout(requestTimeout, TimeUnit.MILLISECONDS)
                    .readTimeout(requestTimeout, TimeUnit.MILLISECONDS)
                    .build();
        }

        // 构建 OkHttp 请求
        Request.Builder builder = new Request.Builder()
                .url(request.fullUrl());

        // 设置 HTTP 方法
        String method = request.method();
        if ("GET".equalsIgnoreCase(method)) {
            builder.get();
        } else if ("POST".equalsIgnoreCase(method)) {
            byte[] body = request.body();
            RequestBody requestBody = body != null
                    ? RequestBody.create(body, MediaType.parse("application/json; charset=utf-8"))
                    : RequestBody.create(new byte[0], MediaType.parse("application/json; charset=utf-8"));
            builder.post(requestBody);
        } else if ("PUT".equalsIgnoreCase(method)) {
            byte[] body = request.body();
            RequestBody requestBody = body != null
                    ? RequestBody.create(body, MediaType.parse("application/json; charset=utf-8"))
                    : RequestBody.create(new byte[0], MediaType.parse("application/json; charset=utf-8"));
            builder.put(requestBody);
        } else if ("DELETE".equalsIgnoreCase(method)) {
            byte[] body = request.body();
            if (body != null) {
                builder.delete(RequestBody.create(body, MediaType.parse("application/json; charset=utf-8")));
            } else {
                builder.delete();
            }
        } else {
            // 默认使用 GET
            builder.get();
        }

        // 添加请求头
        for (Map.Entry<String, List<String>> entry : request.headers().entrySet()) {
            for (String value : entry.getValue()) {
                builder.addHeader(entry.getKey(), value);
            }
        }

        // 执行请求
        try (Response okResponse = client.newCall(builder.build()).execute()) {
            long elapsed = System.currentTimeMillis() - start;

            byte[] bodyBytes = okResponse.body() != null ? okResponse.body().bytes() : new byte[0];

            return new SpiderResponse()
                    .statusCode(okResponse.code())
                    .headers(okResponse.headers().toMultimap())
                    .bodyBytes(bodyBytes)
                    .elapsedMillis(elapsed);
        }
    }
}
