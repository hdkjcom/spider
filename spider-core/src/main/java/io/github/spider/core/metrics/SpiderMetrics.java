package io.github.spider.core.metrics;

import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;

/**
 * 指标记录 SPI。
 * 实现类（Micrometer 等）通过此接口接入，spider-core 无需依赖具体的指标库。
 *
 * <p>使用方式：{@code SpiderClientFactory.builder().metrics(new MicrometerSpiderMetrics(registry))}。
 */
public interface SpiderMetrics {
    default void recordSuccess(String clientName, String methodName, SpiderRequest request, SpiderResponse response) {}
    default void recordFailure(String clientName, String methodName, SpiderRequest request, Exception exception) {}
    default void recordRetry(String clientName, String methodName, int attempt, Exception cause) {}
    default void recordFallback(String clientName, String methodName) {}

    SpiderMetrics NOOP = new SpiderMetrics() {};
}
