package io.github.spider.core.metrics;

import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;

/**
 * SPI for recording invocation metrics.
 * Implementations (Micrometer, etc.) plug in here without spider-core depending on a specific metrics library.
 *
 * <p>Use via {@code SpiderClientFactory.builder().metrics(new MicrometerSpiderMetrics(registry))}.
 */
public interface SpiderMetrics {
    default void recordSuccess(String clientName, String methodName, SpiderRequest request, SpiderResponse response) {}
    default void recordFailure(String clientName, String methodName, SpiderRequest request, Exception exception) {}
    default void recordRetry(String clientName, String methodName, int attempt, Exception cause) {}
    default void recordFallback(String clientName, String methodName) {}

    SpiderMetrics NOOP = new SpiderMetrics() {};
}
