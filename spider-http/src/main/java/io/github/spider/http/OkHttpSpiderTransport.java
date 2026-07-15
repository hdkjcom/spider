package io.github.spider.http;

import io.github.spider.core.codec.SpiderEncoder;
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

    /** request 未声明 content-type 或声明值无法解析时的兜底媒体类型。 */
    private static final MediaType DEFAULT_MEDIA =
            MediaType.parse(SpiderEncoder.DEFAULT_CONTENT_TYPE);

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
            MediaType mediaType = resolveMediaType(request);
            byte[] body = request.body();
            RequestBody requestBody = RequestBody.create(body != null ? body : new byte[0], mediaType);
            builder.post(requestBody);
        } else if ("PUT".equalsIgnoreCase(method)) {
            MediaType mediaType = resolveMediaType(request);
            byte[] body = request.body();
            RequestBody requestBody = RequestBody.create(body != null ? body : new byte[0], mediaType);
            builder.put(requestBody);
        } else if ("DELETE".equalsIgnoreCase(method)) {
            byte[] body = request.body();
            if (body != null) {
                builder.delete(RequestBody.create(body, resolveMediaType(request)));
            } else {
                builder.delete();
            }
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
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

    /**
     * 解析请求的 Content-Type 媒体类型。
     *
     * <p>优先使用 {@link SpiderRequest#contentType()}；未声明时回退到
     * {@link SpiderEncoder#DEFAULT_CONTENT_TYPE}。{@link MediaType#parse(String)} 对格式错误的输入
     * 返回 null（而非抛异常），此处兜底为 {@link #DEFAULT_MEDIA}，避免发出无 Content-Type 的请求。
     */
    private static MediaType resolveMediaType(SpiderRequest request) {
        String raw = request.contentType() != null
                ? request.contentType() : SpiderEncoder.DEFAULT_CONTENT_TYPE;
        MediaType mediaType = MediaType.parse(raw);
        return mediaType != null ? mediaType : DEFAULT_MEDIA;
    }
}
