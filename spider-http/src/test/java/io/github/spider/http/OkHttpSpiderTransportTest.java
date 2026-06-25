package io.github.spider.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class OkHttpSpiderTransportTest {

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

    @Test
    void testGetRequest() throws Exception {
        server.createContext("/users/1", exchange -> {
            byte[] body = "{\"name\":\"test\"}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        startServer();

        SpiderRequest request = new SpiderRequest()
                .method("GET")
                .url("http://localhost:" + port)
                .path("/users/1");

        SpiderResponse response = transport.execute(request);

        assertEquals(200, response.statusCode());
        assertTrue(response.isSuccessful());
        assertEquals("{\"name\":\"test\"}", new String(response.bodyBytes()));
    }

    @Test
    void testPostRequest() throws Exception {
        server.createContext("/users", exchange -> {
            byte[] responseBody = "{\"id\":1}".getBytes();
            exchange.sendResponseHeaders(201, responseBody.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBody);
            }
        });
        startServer();

        SpiderRequest request = new SpiderRequest()
                .method("POST")
                .url("http://localhost:" + port)
                .path("/users")
                .body("{\"name\":\"test\"}".getBytes());

        SpiderResponse response = transport.execute(request);

        assertEquals(201, response.statusCode());
        assertEquals("{\"id\":1}", new String(response.bodyBytes()));
    }

    @Test
    void testHeadersPassedThrough() throws Exception {
        server.createContext("/test", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            String traceId = exchange.getRequestHeaders().getFirst("X-Trace-Id");
            String responseBody = "{\"auth\":\"" + auth + "\",\"trace\":\"" + traceId + "\"}";
            byte[] body = responseBody.getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        startServer();

        SpiderRequest request = new SpiderRequest()
                .method("GET")
                .url("http://localhost:" + port)
                .path("/test")
                .addHeader("Authorization", "Bearer token123")
                .addHeader("X-Trace-Id", "abc-def");

        SpiderResponse response = transport.execute(request);

        assertEquals(200, response.statusCode());
        String body = new String(response.bodyBytes());
        assertTrue(body.contains("Bearer token123"));
        assertTrue(body.contains("abc-def"));
    }

    @Test
    void testElapsedTimeRecorded() throws Exception {
        server.createContext("/ping", exchange -> {
            byte[] body = "ok".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        startServer();

        SpiderRequest request = new SpiderRequest()
                .method("GET")
                .url("http://localhost:" + port)
                .path("/ping");

        SpiderResponse response = transport.execute(request);

        assertTrue(response.elapsedMillis() >= 0);
    }

    @Test
    void testErrorStatusCode() throws Exception {
        server.createContext("/error", exchange -> {
            byte[] body = "Internal Server Error".getBytes();
            exchange.sendResponseHeaders(500, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        startServer();

        SpiderRequest request = new SpiderRequest()
                .method("GET")
                .url("http://localhost:" + port)
                .path("/error");

        SpiderResponse response = transport.execute(request);

        assertEquals(500, response.statusCode());
        assertFalse(response.isSuccessful());
    }
}
