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
 * OkHttp-based SpiderTransport implementation.
 * Converts SpiderRequest to OkHttp Request, executes, converts Response back.
 */
public class OkHttpSpiderTransport implements SpiderTransport {

    private final OkHttpClient httpClient;

    public OkHttpSpiderTransport() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public OkHttpSpiderTransport(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public SpiderResponse execute(SpiderRequest request) throws IOException {
        long start = System.currentTimeMillis();

        // Build OkHttp Request
        Request.Builder builder = new Request.Builder()
                .url(request.fullUrl());

        // Set HTTP method
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
            // Default: GET
            builder.get();
        }

        // Add headers
        for (Map.Entry<String, List<String>> entry : request.headers().entrySet()) {
            for (String value : entry.getValue()) {
                builder.addHeader(entry.getKey(), value);
            }
        }

        // Execute
        Response okResponse = httpClient.newCall(builder.build()).execute();

        long elapsed = System.currentTimeMillis() - start;

        // Convert to SpiderResponse
        byte[] bodyBytes = okResponse.body() != null ? okResponse.body().bytes() : new byte[0];

        return new SpiderResponse()
                .statusCode(okResponse.code())
                .headers(okResponse.headers().toMultimap())
                .bodyBytes(bodyBytes)
                .elapsedMillis(elapsed);
    }
}
