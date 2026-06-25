package io.github.spider.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.spider.core.annotation.*;
import io.github.spider.core.client.SpiderClientFactory;
import io.github.spider.core.client.CountingCircuitBreaker;
import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.policy.SpiderCircuitBreaker;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import io.github.spider.http.OkHttpSpiderTransport;
import io.github.spider.jackson.JacksonSpiderDecoder;
import io.github.spider.jackson.JacksonSpiderEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test: annotation → proxy → metadata → request template → transport → decode → result.
 * This test demonstrates the full Spider request lifecycle.
 */
class SpiderE2ETest {

    private HttpServer server;
    private int port;

    // --- Test client interface ---

    @SpiderClient(name = "test-service", url = "http://localhost:8080")
    interface UserClient {
        @SpiderGet("/users/{id}")
        UserDTO getUser(@Path("id") Long id);

        @SpiderGet("/users")
        UserDTO[] listUsers();

        @SpiderPost("/users")
        @Timeout(2000)
        UserDTO createUser(@Body CreateUserRequest request);
    }

    static class UserDTO {
        public Long id;
        public String name;
        public int age;
    }

    static class CreateUserRequest {
        public String name;
        public int age;

        public CreateUserRequest() {}

        public CreateUserRequest(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    // --- Setup / Teardown ---

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(null);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void startServer() {
        server.start();
        port = server.getAddress().getPort();
    }

    private <T> T createClient(Class<T> clientInterface) {
        String url = "http://localhost:" + port;
        return SpiderClientFactory.builder()
                .transport(new OkHttpSpiderTransport())
                .decoder(new JacksonSpiderDecoder())
                .encoder(new JacksonSpiderEncoder())
                .url(url)
                .build()
                .create(clientInterface);
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] bytes = new byte[4096];
            int len = is.read(bytes);
            return len > 0 ? new String(bytes, 0, len, StandardCharsets.UTF_8) : "";
        }
    }

    // --- Tests ---

    @Test
    void testGetUserById() throws Exception {
        server.createContext("/users/1", exchange -> {
            byte[] body = "{\"id\":1,\"name\":\"zhangsan\",\"age\":28}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        startServer();

        UserClient client = createClient(UserClient.class);
        UserDTO user = client.getUser(1L);

        assertNotNull(user);
        assertEquals(Long.valueOf(1), user.id);
        assertEquals("zhangsan", user.name);
        assertEquals(28, user.age);
    }

    @Test
    void testGetUserById_differentId() throws Exception {
        server.createContext("/users/42", exchange -> {
            byte[] body = "{\"id\":42,\"name\":\"lisi\",\"age\":35}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        startServer();

        UserClient client = createClient(UserClient.class);
        UserDTO user = client.getUser(42L);

        assertNotNull(user);
        assertEquals(Long.valueOf(42), user.id);
        assertEquals("lisi", user.name);
        assertEquals(35, user.age);
    }

    @Test
    void testListUsers() throws Exception {
        server.createContext("/users", exchange -> {
            byte[] body = ("[{\"id\":1,\"name\":\"a\",\"age\":20},"
                    + "{\"id\":2,\"name\":\"b\",\"age\":30}]").getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        startServer();

        UserClient client = createClient(UserClient.class);
        UserDTO[] users = client.listUsers();

        assertNotNull(users);
        assertEquals(2, users.length);
        assertEquals(Long.valueOf(1), users[0].id);
        assertEquals("a", users[0].name);
        assertEquals(Long.valueOf(2), users[1].id);
        assertEquals("b", users[1].name);
    }

    // --- POST + @Body ---

    @Test
    void testCreateUser() throws Exception {
        server.createContext("/users", exchange -> {
            String requestBody = readBody(exchange);
            assertTrue(requestBody.contains("\"name\":\"wangwu\""));
            assertTrue(requestBody.contains("\"age\":22"));

            byte[] body = "{\"id\":3,\"name\":\"wangwu\",\"age\":22}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        startServer();

        UserClient client = createClient(UserClient.class);
        UserDTO user = client.createUser(new CreateUserRequest("wangwu", 22));

        assertNotNull(user);
        assertEquals(Long.valueOf(3), user.id);
        assertEquals("wangwu", user.name);
    }

    // --- Retry ---

    @SpiderClient(name = "retry-service", url = "http://localhost:8080")
    interface RetryClient {
        @SpiderGet("/flaky")
        @Retry(maxAttempts = 3, backoffMillis = 10)
        String getFlaky();
    }

    @Test
    void testRetryOnServerError() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        server.createContext("/flaky", exchange -> {
            int count = callCount.incrementAndGet();
            if (count < 3) {
                byte[] body = "error".getBytes();
                exchange.sendResponseHeaders(503, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            } else {
                byte[] body = "\"success\"".getBytes();
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            }
        });
        startServer();

        RetryClient client = createClient(RetryClient.class);
        String result = client.getFlaky();

        assertEquals("success", result);
        assertEquals(3, callCount.get());
    }

    // --- Fallback ---

    @SpiderClient(name = "fallback-service", url = "http://localhost:8080", fallback = FallbackClientImpl.class)
    public interface FallbackClient {
        @SpiderGet("/unreliable")
        String getUnreliable();
    }

    public static class FallbackClientImpl implements FallbackClient {
        @Override
        public String getUnreliable() {
            return "fallback-data";
        }
    }

    @Test
    void testFallbackOnFailure() throws Exception {
        server.createContext("/unreliable", exchange -> {
            byte[] body = "error".getBytes();
            exchange.sendResponseHeaders(500, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        startServer();

        FallbackClient client = createClient(FallbackClient.class);
        String result = client.getUnreliable();

        assertEquals("fallback-data", result);
    }

    // --- Interceptor ---

    @SpiderClient(name = "interceptor-service", url = "http://localhost:8080")
    interface InterceptorClient {
        @SpiderGet("/hello")
        String hello();
    }

    @Test
    void testInterceptorAddsHeader() throws Exception {
        server.createContext("/hello", exchange -> {
            String traceHeader = exchange.getRequestHeaders().getFirst("X-Trace-Id");
            byte[] body = ("\"trace:" + traceHeader + "\"").getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        startServer();

        String url = "http://localhost:" + port;
        InterceptorClient client = SpiderClientFactory.builder()
                .transport(new OkHttpSpiderTransport())
                .decoder(new JacksonSpiderDecoder())
                .addInterceptor(new SpiderInterceptor() {
                    @Override
                    public SpiderRequest beforeRequest(SpiderRequest request) {
                        request.addHeader("X-Trace-Id", "trace-abc-123");
                        return request;
                    }
                })
                .url(url)
                .build()
                .create(InterceptorClient.class);

        String result = client.hello();
        assertEquals("trace:trace-abc-123", result);
    }

    // --- Circuit Breaker ---

    @SpiderClient(name = "cb-service", url = "http://localhost:8080")
    interface CircuitBreakerClient {
        @SpiderGet("/ping")
        String ping();
    }

    @Test
    void testCircuitBreakerOpensAfterFailures() throws Exception {
        // All requests return 500
        AtomicInteger callCount = new AtomicInteger(0);
        server.createContext("/ping", exchange -> {
            callCount.incrementAndGet();
            byte[] body = "error".getBytes();
            exchange.sendResponseHeaders(500, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        startServer();

        String url = "http://localhost:" + port;
        CountingCircuitBreaker cb = new CountingCircuitBreaker(1, 5, 500, 2); // low threshold for testing

        CircuitBreakerClient client = SpiderClientFactory.builder()
                .transport(new OkHttpSpiderTransport())
                .decoder(new JacksonSpiderDecoder())
                .circuitBreaker(cb)
                .url(url)
                .build()
                .create(CircuitBreakerClient.class);

        // Trigger failures until circuit opens
        for (int i = 0; i < 10; i++) {
            try { client.ping(); } catch (Exception ignored) {}
        }

        assertEquals(SpiderCircuitBreaker.State.OPEN, cb.state());
    }
}
