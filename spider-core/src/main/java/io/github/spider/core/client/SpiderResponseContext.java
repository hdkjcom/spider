package io.github.spider.core.client;

import io.github.spider.core.transport.SpiderResponse;

/**
 * Provides access to response metadata (headers, status code) after a Spider call.
 * Uses a ThreadLocal, so it is safe for per-request use in thread-per-request models.
 *
 * <pre>{@code
 * UserDTO user = client.getUser(1L);
 * SpiderResponse resp = SpiderResponseContext.lastResponse();
 * String requestId = resp.headers().get("X-Request-Id").get(0);
 * }</pre>
 *
 * <p>In reactive or async code the caller must capture the response immediately
 * after the call, before switching threads.
 */
public final class SpiderResponseContext {

    private static final ThreadLocal<SpiderResponse> CURRENT = new ThreadLocal<>();

    /** Called by the framework after each transport execution. */
    public static void set(SpiderResponse response) { CURRENT.set(response); }

    /** Returns the last response from a Spider call on this thread, or null. */
    public static SpiderResponse lastResponse() { return CURRENT.get(); }

    /** Called by the framework to clear after each invocation. */
    public static void clear() { CURRENT.remove(); }

    private SpiderResponseContext() {}
}
