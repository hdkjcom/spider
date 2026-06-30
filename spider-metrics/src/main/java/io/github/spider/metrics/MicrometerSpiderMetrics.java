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
import java.util.function.Supplier;

/**
 * Micrometer-based SpiderMetrics implementation.
 *
 * <p>Metric names (stable):
 * <ul>
 *   <li>{@code spider.client.requests} — total invocations (tag: outcome=success|failure)</li>
 *   <li>{@code spider.client.retries} — retry attempts (tag: error_type)</li>
 *   <li>{@code spider.client.fallbacks} — fallback activations</li>
 *   <li>{@code spider.client.duration} — invocation duration histogram (tag: outcome=success;
 *       only successful invocations record duration, since failure path has no timing input)</li>
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
        getOrCreate(successCounters, key, () -> Counter.builder("spider.client.requests")
                .tag("client", clientName).tag("method", methodName)
                .tag("outcome", "success")
                .description("Successful Spider invocations")
                .register(registry)).increment();
        getOrCreate(timers, key(clientName, methodName, "success"), () -> Timer.builder("spider.client.duration")
                .tag("client", clientName).tag("method", methodName)
                .tag("outcome", "success")
                .description("Spider invocation duration")
                .register(registry)).record(response.elapsedMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordFailure(String clientName, String methodName, SpiderRequest request, Exception exception) {
        String errorType = exception != null ? exception.getClass().getSimpleName() : "unknown";
        String key = key(clientName, methodName, "failure", errorType);
        getOrCreate(failureCounters, key, () -> Counter.builder("spider.client.requests")
                .tag("client", clientName).tag("method", methodName)
                .tag("outcome", "failure").tag("error_type", errorType)
                .description("Failed Spider invocations")
                .register(registry)).increment();
    }

    @Override
    public void recordRetry(String clientName, String methodName, int attempt, Exception cause) {
        String errorType = cause != null ? cause.getClass().getSimpleName() : "unknown";
        String key = key(clientName, methodName, "retry", errorType);
        getOrCreate(retryCounters, key, () -> Counter.builder("spider.client.retries")
                .tag("client", clientName).tag("method", methodName)
                .tag("error_type", errorType)
                .description("Spider retry attempts")
                .register(registry)).increment();
    }

    @Override
    public void recordFallback(String clientName, String methodName) {
        String key = key(clientName, methodName);
        getOrCreate(fallbackCounters, key, () -> Counter.builder("spider.client.fallbacks")
                .tag("client", clientName).tag("method", methodName)
                .description("Spider fallback invocations")
                .register(registry)).increment();
    }

    /**
     * 线程安全的 get-or-create，避免 {@code computeIfAbsent} 在高并发下重复注册 Meter。
     */
    private static <T> T getOrCreate(ConcurrentMap<String, T> map, String key, Supplier<T> factory) {
        T existing = map.get(key);
        if (existing != null) return existing;
        T created = factory.get();
        T prev = map.putIfAbsent(key, created);
        return prev != null ? prev : created;
    }

    private static String key(String... parts) {
        return String.join(".", parts);
    }
}
