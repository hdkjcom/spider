package io.github.spider.metrics;

import io.github.spider.core.metrics.SpiderMetrics;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MicrometerSpiderMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MicrometerSpiderMetrics metrics = new MicrometerSpiderMetrics(registry);

    @Test
    void testRecordSuccess() {
        SpiderRequest request = new SpiderRequest().method("GET").url("http://localhost:8080");
        SpiderResponse response = new SpiderResponse().statusCode(200).elapsedMillis(42);

        metrics.recordSuccess("test-client", "getUser", request, response);

        // Verify counters were incremented
        double successCount = registry.get("spider.requests.success")
                .tag("client", "test-client")
                .tag("method", "getUser")
                .counter().count();
        assertEquals(1.0, successCount);

        // Verify timer recorded
        double totalTime = registry.get("spider.requests.duration")
                .tag("client", "test-client")
                .tag("method", "getUser")
                .timer().totalTime(TimeUnit.MILLISECONDS);
        assertTrue(totalTime >= 42);
    }

    @Test
    void testRecordFailure() {
        metrics.recordFailure("test-client", "getUser", new SpiderRequest(),
                new RuntimeException("connection refused"));

        double failureCount = registry.get("spider.requests.failure")
                .tag("client", "test-client")
                .tag("method", "getUser")
                .counter().count();
        assertEquals(1.0, failureCount);
    }

    @Test
    void testRecordRetry() {
        metrics.recordRetry("test-client", "getUser", 1,
                new RuntimeException("timeout"));

        double retryCount = registry.get("spider.requests.retry")
                .tag("client", "test-client")
                .tag("method", "getUser")
                .counter().count();
        assertEquals(1.0, retryCount);
    }

    @Test
    void testRecordFallback() {
        metrics.recordFallback("test-client", "getUser");

        double fallbackCount = registry.get("spider.requests.fallback")
                .tag("client", "test-client")
                .tag("method", "getUser")
                .counter().count();
        assertEquals(1.0, fallbackCount);
    }

    @Test
    void testMultipleRecords() {
        SpiderResponse resp = new SpiderResponse().statusCode(200).elapsedMillis(10);

        metrics.recordSuccess("svc", "a", new SpiderRequest(), resp);
        metrics.recordSuccess("svc", "a", new SpiderRequest(), resp);
        metrics.recordSuccess("svc", "b", new SpiderRequest(), resp);

        assertEquals(2.0, registry.get("spider.requests.success").tag("method", "a").counter().count());
        assertEquals(1.0, registry.get("spider.requests.success").tag("method", "b").counter().count());
    }

    @Test
    void testNoopMetricsDoesNotThrow() {
        SpiderMetrics noop = SpiderMetrics.NOOP;
        // All methods should be safe to call
        noop.recordSuccess("c", "m", null, null);
        noop.recordFailure("c", "m", null, null);
        noop.recordRetry("c", "m", 1, null);
        noop.recordFallback("c", "m");
    }
}
