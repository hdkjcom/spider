package io.github.spider.core.policy;

/**
 * Factory for creating fallback instances with access to the failure cause.
 *
 * <pre>{@code
 * public class PayClientFallbackFactory implements FallbackFactory<PayClient> {
 *     public PayClient create(Throwable cause) {
 *         log.warn("PayClient fallback triggered: {}", cause.getMessage());
 *         return id -> PayResult.empty(id);
 *     }
 * }
 *
 * @SpiderClient(name = "pay", url = "...", fallbackFactory = PayClientFallbackFactory.class)
 * }</pre>
 *
 * @param <T> the client interface type
 */
public interface FallbackFactory<T> {
    /** Create a fallback instance for the given failure. */
    T create(Throwable cause);
}
