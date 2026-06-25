package io.github.spider.core.interceptor;

import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;

/**
 * Interceptor for the Spider request/response pipeline.
 *
 * <p>Interceptors are called in registration order. Use them for
 * cross-cutting concerns: authentication headers, logging, tracing, metrics.
 *
 * <pre>{@code
 * SpiderClientFactory.builder()
 *     .addInterceptor(new SpiderInterceptor() {
 *         public SpiderRequest beforeRequest(SpiderRequest req) {
 *             req.addHeader("Authorization", token());
 *             return req;
 *         }
 *     })
 *     .build();
 * }</pre>
 */
public interface SpiderInterceptor {

    /**
     * Called before the transport executes.
     * Return a (possibly modified) request. Throw to abort the call.
     */
    default SpiderRequest beforeRequest(SpiderRequest request) { return request; }

    /**
     * Called after the transport returns a successful response.
     * Return a (possibly modified) response.
     */
    default SpiderResponse afterResponse(SpiderResponse response) { return response; }

    /**
     * Called when the request fails after all retries are exhausted.
     * Return {@code true} to suppress the exception (caller receives null).
     */
    default boolean onError(SpiderRequest request, Exception ex) { return false; }
}
