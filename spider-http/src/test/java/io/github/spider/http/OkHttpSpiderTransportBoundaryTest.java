package io.github.spider.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boundary tests for {@link OkHttpSpiderTransport} using the JDK's built-in
 * {@code com.sun.net.httpserver.HttpServer} (same approach as the existing
 * {@code OkHttpSpiderTransportTest}).
 *
 * <p>Covers areas not exercised by the base test class:
 * <ul>
 *   <li>Request header passthrough (single + multi-value)</li>
 *   <li>Query string propagation through the full URL</li>
 *   <li>POST request body content delivered verbatim to the server</li>
 *   <li>HTTP 404 and HTTP 500 status codes surfaced on the SpiderResponse</li>
 *   <li>Per-request timeout interrupts a deliberately slow response</li>
 * </ul>
 *
 * <p>Note on query params: {@link SpiderRequest#addQueryParam(String, String)}
 * is currently not read by the transport, so the query-string test embeds the
 * query directly in the URL to reflect real transport behavior.
 */
class OkHttpSpiderTransportBoundaryTest {

    private HttpServer server;
    private OkHttpSpiderTransport transport;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(null);
        transport = new OkHttpSpiderTransport();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void startServer() throws IOException {
        server.start();
        port = server.getAddress().getPort();
    }

    /** Small helper to drain the request body off the wire. */
    private static byte[] readRequestBody(HttpExchange exchange) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int remaining = exchange.getRequestHeaders().containsKey("Content-Length")
                    ? Integer.parseInt(exchange.getRequestHeaders().getFirst("Content-Length"))
                    : 0;
            try (java.io.InputStream is = exchange.getRequestBody()) {
                while (remaining > 0) {
                    int read = is.read(buffer, 0, Math.min(buffer.length, remaining));
                    if (read == -1) {
                        break;
                    }
                    baos.write(buffer, 0, read);
                    remaining -= read;
                }
            }
            return baos.toByteArray();
        }
    }

    private static void sendResponse(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    // ---- Header passthrough ----

    @Test
    void testHeadersPassedThroughToServer() throws Exception {
        server.createContext("/headers", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            String traceId = exchange.getRequestHeaders().getFirst("X-Trace-Id");
            String custom = exchange.getRequestHeaders().getFirst("X-Custom");
            String responseBody = String.format(
                    "{\"auth\":\"%s\",\"trace\":\"%s\",\"custom\":\"%s\"}",
                    auth, traceId, custom);
            sendResponse(exchange, 200, responseBody.getBytes(StandardCharsets.UTF_8));
        });
        startServer();

        SpiderRequest request = new SpiderRequest()
                .method("GET")
                .url("http://localhost:" + port)
                .path("/headers")
                .addHeader("Authorization", "Bearer abc-123")
                .addHeader("X-Trace-Id", "trace-xyz")
                .addHeader("X-Custom", "custom-value");

        SpiderResponse response = transport.execute(request);

        assertEquals(200, response.statusCode());
        String body = new String(response.bodyBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("Bearer abc-123"), "Authorization header must reach the server");
        assertTrue(body.contains("trace-xyz"), "Trace header must reach the server");
        assertTrue(body.contains("custom-value"), "Custom header must reach the server");
    }

    @Test
    void testMultiValueHeaderPassedThrough() throws Exception {
        server.createContext("/multi", exchange -> {
            // Echo back the raw header values for "X-Accept".
            List<String> values = exchange.getRequestHeaders().get("X-Accept");
            String joined = values == null ? "" : String.join(",", values);
            sendResponse(exchange, 200, joined.getBytes(StandardCharsets.UTF_8));
        });
        startServer();

        SpiderRequest request = new SpiderRequest()
                .method("GET")
                .url("http://localhost:" + port)
                .path("/multi")
                .addHeader("X-Accept", "one")
                .addHeader("X-Accept", "two");

        SpiderResponse response = transport.execute(request);

        assertEquals(200, response.statusCode());
        String body = new String(response.bodyBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("one"), "First header value must reach the server");
        assertTrue(body.contains("two"), "Second header value must reach the server");
    }

    // ---- Query string ----

    @Test
    void testQueryStringInUrlReceivedByServer() throws Exception {
        server.createContext("/search", exchange -> {
            // Echo the raw query string back to the client.
            String query = exchange.getRequestURI().getQuery();
            byte[] body = (query == null ? "" : query).getBytes(StandardCharsets.UTF_8);
            sendResponse(exchange, 200, body);
        });
        startServer();

        // The transport currently surfaces the query string only when it is
        // embedded in the URL/path itself (SpiderRequest#addQueryParam is not
        // read by OkHttpSpiderTransport). Place the query on the path to
        // exercise the supported code path.
        SpiderRequest request = new SpiderRequest()
                .method("GET")
                .url("http://localhost:" + port)
                .path("/search")
                .url("http://localhost:" + port + "/search?q=spider&page=1")
                .path("");

        SpiderResponse response = transport.execute(request);

        assertEquals(200, response.statusCode());
        String body = new String(response.bodyBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("q=spider"),
                "Query param 'q' must be received by the server, was: " + body);
        assertTrue(body.contains("page=1"),
                "Query param 'page' must be received by the server, was: " + body);
    }

    // ---- POST body ----

    @Test
    void testPostBodyDeliveredVerbatim() throws Exception {
        server.createContext("/echo", exchange -> {
            byte[] requestBody = readRequestBody(exchange);
            // Echo the exact request body back as the response body.
            sendResponse(exchange, 200, requestBody);
        });
        startServer();

        String payload = "{\"name\":\"susan\",\"age\":42}";
        SpiderRequest request = new SpiderRequest()
                .method("POST")
                .url("http://localhost:" + port)
                .path("/echo")
                .body(payload.getBytes(StandardCharsets.UTF_8));

        SpiderResponse response = transport.execute(request);

        assertEquals(200, response.statusCode());
        assertEquals(payload, new String(response.bodyBytes(), StandardCharsets.UTF_8),
                "Server must receive the POST body exactly as sent");
    }

    @Test
    void testPostBodyWithSpecialCharacters() throws Exception {
        server.createContext("/echo-utf8", exchange -> {
            byte[] requestBody = readRequestBody(exchange);
            sendResponse(exchange, 200, requestBody);
        });
        startServer();

        String payload = "{\"msg\":\"中文 / emoji ✓\",\"value\":1}";
        SpiderRequest request = new SpiderRequest()
                .method("POST")
                .url("http://localhost:" + port)
                .path("/echo-utf8")
                .body(payload.getBytes(StandardCharsets.UTF_8));

        SpiderResponse response = transport.execute(request);

        assertEquals(200, response.statusCode());
        assertEquals(payload, new String(response.bodyBytes(), StandardCharsets.UTF_8),
                "UTF-8 multi-byte POST body must round-trip correctly");
    }

    // ---- Error status codes ----

    @Test
    void testNotFoundStatusCodeSurfaced() throws Exception {
        server.createContext("/missing", exchange -> {
            byte[] body = "not found".getBytes(StandardCharsets.UTF_8);
            sendResponse(exchange, 404, body);
        });
        startServer();

        SpiderRequest request = new SpiderRequest()
                .method("GET")
                .url("http://localhost:" + port)
                .path("/missing");

        SpiderResponse response = transport.execute(request);

        assertEquals(404, response.statusCode());
        assertFalse(response.isSuccessful(), "404 must not be flagged as successful");
    }

    @Test
    void testInternalServerErrorStatusCodeSurfaced() throws Exception {
        server.createContext("/boom", exchange -> {
            byte[] body = "Internal Server Error".getBytes(StandardCharsets.UTF_8);
            sendResponse(exchange, 500, body);
        });
        startServer();

        SpiderRequest request = new SpiderRequest()
                .method("GET")
                .url("http://localhost:" + port)
                .path("/boom");

        SpiderResponse response = transport.execute(request);

        assertEquals(500, response.statusCode());
        assertFalse(response.isSuccessful(), "500 must not be flagged as successful");
        assertEquals("Internal Server Error",
                new String(response.bodyBytes(), StandardCharsets.UTF_8));
    }

    // ---- Per-request timeout ----

    @Test
    void testPerRequestTimeoutInterruptsSlowResponse() throws Exception {
        // Server sleeps longer than the per-request timeout configured below.
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
            byte[] body = "too late".getBytes(StandardCharsets.UTF_8);
            sendResponse(exchange, 200, body);
        });
        startServer();

        SpiderRequest request = new SpiderRequest()
                .method("GET")
                .url("http://localhost:" + port)
                .path("/slow")
                .timeoutMillis(300);

        long start = System.currentTimeMillis();
        IOException thrown = assertThrows(IOException.class,
                () -> transport.execute(request));
        long elapsed = System.currentTimeMillis() - start;

        // The configured 300ms timeout must abort the call well before the
        // 3s server delay. Allow generous slack to avoid CI flakiness.
        assertTrue(elapsed < 2500,
                "Request should be aborted by the per-request timeout, took " + elapsed + "ms");
        assertNotNull(thrown.getMessage(), "Timeout should propagate as an IOException");
    }

    @Test
    void testResponseHeadersMapPopulated() throws Exception {
        server.createContext("/with-headers", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("X-Server", "spider-test");
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            sendResponse(exchange, 200, body);
        });
        startServer();

        SpiderRequest request = new SpiderRequest()
                .method("GET")
                .url("http://localhost:" + port)
                .path("/with-headers");

        SpiderResponse response = transport.execute(request);

        assertEquals(200, response.statusCode());
        Map<String, List<String>> headers = response.headers();
        assertNotNull(headers, "Response headers map must not be null");
        // OkHttp lowercases header names in its multimap.
        assertNotNull(headers.get("content-type"),
                "Content-Type header must be present on the response");
        assertNotNull(headers.get("x-server"),
                "Custom X-Server header must be present on the response");
    }
}
