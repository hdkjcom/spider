package io.github.spider.metrics;

import io.github.spider.core.metrics.SpiderMetrics;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Micrometer-based SpiderMetrics implementation.
 * Records invocation metrics via Micrometer's MeterRegistry.
 */
public class MicrometerSpiderMetrics implements SpiderMetrics {

    private final MeterRegistry registry;
    private final ConcurrentMap<String, Counter> successCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> failureCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> retryCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> fallbackCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public MicrometerSpiderMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordSuccess(String clientName, String methodName, SpiderRequest request, SpiderResponse response) {
        String key = key(clientName, methodName);
        successCounters.computeIfAbsent(key,
                k -> Counter.builder("spider.requests.success")
                        .tag("client", clientName)
                        .tag("method", methodName)
                        .description("Successful Spider invocations")
                        .register(registry)).increment();
        timers.computeIfAbsent(key,
                k -> Timer.builder("spider.requests.duration")
                        .tag("client", clientName)
                        .tag("method", methodName)
                        .description("Spider invocation duration")
                        .register(registry)).record(response.elapsedMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordFailure(String clientName, String methodName, SpiderRequest request, Exception exception) {
        failureCounters.computeIfAbsent(key(clientName, methodName),
                k -> Counter.builder("spider.requests.failure")
                        .tag("client", clientName)
                        .tag("method", methodName)
                        .description("Failed Spider invocations")
                        .register(registry)).increment();
    }

    @Override
    public void recordRetry(String clientName, String methodName, int attempt, Exception cause) {
        retryCounters.computeIfAbsent(key(clientName, methodName),
                k -> Counter.builder("spider.requests.retry")
                        .tag("client", clientName)
                        .tag("method", methodName)
                        .description("Spider retry attempts")
                        .register(registry)).increment();
    }

    @Override
    public void recordFallback(String clientName, String methodName) {
        fallbackCounters.computeIfAbsent(key(clientName, methodName),
                k -> Counter.builder("spider.requests.fallback")
                        .tag("client", clientName)
                        .tag("method", methodName)
                        .description("Spider fallback invocations")
                        .register(registry)).increment();
    }

    private static String key(String clientName, String methodName) {
        return clientName + "." + methodName;
    }
}
