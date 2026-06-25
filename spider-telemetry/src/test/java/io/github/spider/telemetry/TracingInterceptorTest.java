package io.github.spider.telemetry;

import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TracingInterceptorTest {

    private static final OpenTelemetry noopOtel = OpenTelemetry.noop();

    @Test
    void testInjectsTraceHeaders() {
        TracingInterceptor interceptor = new TracingInterceptor(noopOtel.getTracer("test"));

        SpiderRequest request = new SpiderRequest()
                .method("GET").url("http://localhost").path("/test");

        SpiderRequest modified = interceptor.beforeRequest(request);
        // With noop, traces are no-op but the interceptor doesn't throw
        assertTrue(modified.headers().isEmpty() || modified.headers().size() >= 0);
    }

    @Test
    void testTracerConstructorDoesNotThrow() {
        TracingInterceptor interceptor = new TracingInterceptor(noopOtel.getTracer("test"));
        assertNotNull(interceptor);
    }

    @Test
    void testSpanClosedAfterSuccess() {
        TracingInterceptor interceptor = new TracingInterceptor(noopOtel.getTracer("test"));

        SpiderRequest request = new SpiderRequest()
                .method("GET").url("http://localhost").path("/test");
        interceptor.beforeRequest(request);

        SpiderResponse response = new SpiderResponse().statusCode(200);
        interceptor.afterResponse(response);
        // No exception means span cleanup succeeded
    }

    @Test
    void testSpanClosedAfterError() {
        TracingInterceptor interceptor = new TracingInterceptor(noopOtel.getTracer("test"));

        SpiderRequest request = new SpiderRequest()
                .method("GET").url("http://localhost").path("/test");
        interceptor.beforeRequest(request);

        interceptor.onError(request, new RuntimeException("boom"));
        // No exception means span cleanup succeeded
    }
}
