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
 *
 * <p>Metric names (stable):
 * <ul>
 *   <li>{@code spider.client.requests} — total invocations (tag: outcome=success|failure)</li>
 *   <li>{@code spider.client.retries} — retry attempts (tag: error_type)</li>
 *   <li>{@code spider.client.fallbacks} — fallback activations</li>
 *   <li>{@code spider.client.duration} — invocation duration histogram</li>
 * </ul>
 *
 * <p>Tags (low-cardinality only):
 * {@code client}, {@code method}, {@code outcome}, {@code error_type}.
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
        String key = key(clientName, methodName, "success");
        successCounters.computeIfAbsent(key,
                k -> Counter.builder("spider.client.requests")
                        .tag("client", clientName)
                        .tag("method", methodName)
                        .tag("outcome", "success")
                        .description("Successful Spider invocations")
                        .register(registry)).increment();
        timers.computeIfAbsent(key(clientName, methodName),
                k -> Timer.builder("spider.client.duration")
                        .tag("client", clientName)
                        .tag("method", methodName)
                        .description("Spider invocation duration")
                        .register(registry)).record(response.elapsedMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordFailure(String clientName, String methodName, SpiderRequest request, Exception exception) {
        String errorType = exception != null ? exception.getClass().getSimpleName() : "unknown";
        failureCounters.computeIfAbsent(key(clientName, methodName, "failure", errorType),
                k -> Counter.builder("spider.client.requests")
                        .tag("client", clientName)
                        .tag("method", methodName)
                        .tag("outcome", "failure")
                        .tag("error_type", errorType)
                        .description("Failed Spider invocations")
                        .register(registry)).increment();
    }

    @Override
    public void recordRetry(String clientName, String methodName, int attempt, Exception cause) {
        String errorType = cause != null ? cause.getClass().getSimpleName() : "unknown";
        retryCounters.computeIfAbsent(key(clientName, methodName, "retry", errorType),
                k -> Counter.builder("spider.client.retries")
                        .tag("client", clientName)
                        .tag("method", methodName)
                        .tag("error_type", errorType)
                        .description("Spider retry attempts")
                        .register(registry)).increment();
    }

    @Override
    public void recordFallback(String clientName, String methodName) {
        fallbackCounters.computeIfAbsent(key(clientName, methodName),
                k -> Counter.builder("spider.client.fallbacks")
                        .tag("client", clientName)
                        .tag("method", methodName)
                        .description("Spider fallback invocations")
                        .register(registry)).increment();
    }

    private static String key(String... parts) {
        return String.join(".", parts);
    }
}
