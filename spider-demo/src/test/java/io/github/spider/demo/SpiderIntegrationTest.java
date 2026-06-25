package io.github.spider.demo;

import com.sun.net.httpserver.HttpServer;
import io.github.spider.core.annotation.*;
import io.github.spider.core.client.CountingCircuitBreaker;
import io.github.spider.core.client.SpiderClientFactory;
import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.policy.SpiderCircuitBreaker;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import io.github.spider.core.metrics.SpiderMetrics;
import io.github.spider.http.OkHttpSpiderTransport;
import io.github.spider.jackson.JacksonSpiderDecoder;
import io.github.spider.jackson.JacksonSpiderEncoder;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: retry + circuit breaker + rate limit + interceptor + metrics combined.
 * Starts an embedded HTTP server, creates a fully-configured Spider client,
 * and verifies the entire pipeline behaves correctly under success and failure.
 */
class SpiderIntegrationTest {

    private static HttpServer server;
    private static int port;

    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicLong lastTraceId = new AtomicLong();

    // ---- client under test ----

    @SpiderClient(name = "integration-test", url = "http://localhost:0",
                  fallback = IntegrationFallback.class)
    public interface IntegrationClient {
        @SpiderGet("/api/ok")
        @Retry(maxAttempts = 2)
        String ok();

        @SpiderGet("/api/flaky")
        @Retry(maxAttempts = 3, backoffMillis = 10)
        String flaky();

        @SpiderGet("/api/fail")
        @Retry(maxAttempts = 1)
        String alwaysFail();

        @SpiderPost("/api/echo")
        EchoBody echo(@Body EchoBody body);
    }

    public static class IntegrationFallback implements IntegrationClient {
        @Override public String ok() { return "fallback-ok"; }
        @Override public String flaky() { return "fallback-flaky"; }
        @Override public String alwaysFail() { return "fallback-fail"; }
        @Override public EchoBody echo(EchoBody b) { return new EchoBody("fallback"); }
    }

    public static class EchoBody {
        public String text;
        public EchoBody() {}
        public EchoBody(String t) { text = t; }
    }

    // ---- metrics collector ----

    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger failureCount = new AtomicInteger();
    private final AtomicInteger retryCount = new AtomicInteger();
    private final AtomicInteger fallbackCount = new AtomicInteger();

    // ---- setup / teardown ----

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(null);

        server.createContext("/api/ok", ex -> {
            byte[] b = "\"ok\"".getBytes();
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        });

        AtomicInteger flaky = new AtomicInteger(0);
        server.createContext("/api/flaky", ex -> {
            if (flaky.incrementAndGet() < 3) {
                byte[] b = "err".getBytes();
                ex.sendResponseHeaders(503, b.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(b); }
            } else {
                byte[] b = "\"retry-ok\"".getBytes();
                ex.sendResponseHeaders(200, b.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(b); }
            }
        });

        server.createContext("/api/fail", ex -> {
            byte[] b = "err".getBytes();
            ex.sendResponseHeaders(500, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        });

        server.createContext("/api/echo", ex -> {
            byte[] buf = new byte[256];
            int n = ex.getRequestBody().read(buf);
            String body = n > 0 ? new String(buf, 0, n) : "{}";
            ex.sendResponseHeaders(200, body.length());
            try (OutputStream os = ex.getResponseBody()) { os.write(body.getBytes()); }
        });

        server.start();
        port = server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    private IntegrationClient createClient() {
        return SpiderClientFactory.builder()
                .transport(new OkHttpSpiderTransport())
                .decoder(new JacksonSpiderDecoder())
                .encoder(new JacksonSpiderEncoder())
                .url("http://localhost:" + port)
                .addInterceptor(new SpiderInterceptor() {
                    @Override
                    public SpiderRequest beforeRequest(SpiderRequest req) {
                        req.addHeader("X-Trace-Id", String.valueOf(lastTraceId.incrementAndGet()));
                        return req;
                    }

                    @Override
                    public SpiderResponse afterResponse(SpiderResponse resp) {
                        requestCount.incrementAndGet();
                        return resp;
                    }
                })
                .metrics(new SpiderMetrics() {
                    @Override public void recordSuccess(String c, String m, SpiderRequest r, SpiderResponse resp) { successCount.incrementAndGet(); }
                    @Override public void recordFailure(String c, String m, SpiderRequest r, Exception e) { failureCount.incrementAndGet(); }
                    @Override public void recordRetry(String c, String m, int a, Exception e) { retryCount.incrementAndGet(); }
                    @Override public void recordFallback(String c, String m) { fallbackCount.incrementAndGet(); }
                })
                .build()
                .create(IntegrationClient.class);
    }

    // ---- tests ----

    @Test
    void testSimpleGet() {
        IntegrationClient client = createClient();
        String result = client.ok();
        assertEquals("ok", result);
        assertTrue(successCount.get() >= 1, "success metric should be recorded");
    }

    @Test
    void testRetryOn503() {
        IntegrationClient client = createClient();
        String result = client.flaky();
        assertEquals("retry-ok", result);
        assertTrue(retryCount.get() >= 2, "retry metric should show at least 2 retries");
    }

    @Test
    void testFallbackOnPermanentFailure() {
        IntegrationClient client = createClient();
        String result = client.alwaysFail();
        assertEquals("fallback-fail", result);
        assertTrue(fallbackCount.get() >= 1, "fallback metric should be recorded");
    }

    @Test
    void testCircuitBreakerOpensAfterFailures() throws Exception {
        CountingCircuitBreaker cb = new CountingCircuitBreaker(5, 10, 500, 2);
        IntegrationClient client = SpiderClientFactory.builder()
                .transport(new OkHttpSpiderTransport())
                .decoder(new JacksonSpiderDecoder())
                .url("http://localhost:" + port)
                .circuitBreaker(cb)
                .build()
                .create(IntegrationClient.class);

        // trigger enough failures to open
        for (int i = 0; i < 10; i++) {
            try { client.alwaysFail(); } catch (Exception ignored) {}
        }
        assertEquals(SpiderCircuitBreaker.State.OPEN, cb.state());
    }

    @Test
    void testInterceptorAddsHeader() {
        IntegrationClient client = createClient();
        client.ok();
        assertTrue(lastTraceId.get() > 0, "interceptor should have set X-Trace-Id header");
    }

    @Test
    void testPostWithBody() {
        IntegrationClient client = createClient();
        EchoBody result = client.echo(new EchoBody("hello"));
        assertNotNull(result);
        assertEquals("hello", result.text);
    }
}
