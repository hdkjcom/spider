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

        double successCount = registry.get("spider.client.requests")
                .tag("client", "test-client")
                .tag("method", "getUser")
                .tag("outcome", "success")
                .counter().count();
        assertEquals(1.0, successCount);

        double totalTime = registry.get("spider.client.duration")
                .tag("client", "test-client")
                .tag("method", "getUser")
                .tag("outcome", "success")
                .timer().totalTime(TimeUnit.MILLISECONDS);
        assertTrue(totalTime >= 42);
    }

    @Test
    void testDurationTimerHasOutcomeSuccessTag() {
        SpiderResponse response = new SpiderResponse().statusCode(200).elapsedMillis(15);

        metrics.recordSuccess("test-client", "getUser", new SpiderRequest(), response);

        // duration Timer must carry outcome=success so success vs failure latency can be told apart
        assertEquals("success", registry.get("spider.client.duration")
                .tag("client", "test-client")
                .tag("method", "getUser")
                .timer().getId().getTag("outcome"));

        // outcome=success must be the only outcome series for duration so far (failure path records no duration)
        assertEquals(1, registry.get("spider.client.duration")
                .tag("client", "test-client")
                .tag("method", "getUser")
                .timers().size());
    }

    @Test
    void testRecordFailure() {
        metrics.recordFailure("test-client", "getUser", new SpiderRequest(),
                new RuntimeException("connection refused"));

        double failureCount = registry.get("spider.client.requests")
                .tag("client", "test-client")
                .tag("method", "getUser")
                .tag("outcome", "failure")
                .tag("error_type", "RuntimeException")
                .counter().count();
        assertEquals(1.0, failureCount);
    }

    @Test
    void testRecordRetry() {
        metrics.recordRetry("test-client", "getUser", 1,
                new RuntimeException("timeout"));

        double retryCount = registry.get("spider.client.retries")
                .tag("client", "test-client")
                .tag("method", "getUser")
                .tag("error_type", "RuntimeException")
                .counter().count();
        assertEquals(1.0, retryCount);
    }

    @Test
    void testRecordFallback() {
        metrics.recordFallback("test-client", "getUser");

        double fallbackCount = registry.get("spider.client.fallbacks")
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

        assertEquals(2.0, registry.get("spider.client.requests")
                .tag("method", "a").tag("outcome", "success").counter().count());
        assertEquals(1.0, registry.get("spider.client.requests")
                .tag("method", "b").tag("outcome", "success").counter().count());
    }

    @Test
    void testNoopMetricsDoesNotThrow() {
        SpiderMetrics noop = SpiderMetrics.NOOP;
        noop.recordSuccess("c", "m", null, null);
        noop.recordFailure("c", "m", null, null);
        noop.recordRetry("c", "m", 1, null);
        noop.recordFallback("c", "m");
    }
}
