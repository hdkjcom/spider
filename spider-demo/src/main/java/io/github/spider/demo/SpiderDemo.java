package io.github.spider.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.spider.core.annotation.*;
import io.github.spider.core.client.CountingCircuitBreaker;
import io.github.spider.core.client.SpiderClientFactory;
import io.github.spider.core.client.SpiderClientException;
import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.http.OkHttpSpiderTransport;
import io.github.spider.jackson.JacksonSpiderDecoder;
import io.github.spider.jackson.JacksonSpiderEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spider 自包含演示程序。
 * 内置 HTTP Server，启动后自动演示声明式调用、重试、降级、熔断、拦截器。
 */
public class SpiderDemo {

    private static HttpServer server;
    private static int port;
    private static final AtomicInteger flakyCounter = new AtomicInteger(0);

    @SpiderClient(name = "user-service", url = "http://localhost:0")
    public interface UserClient {
        @SpiderGet("/users/{id}") @Timeout(2000) UserDTO getUser(@Path("id") Long id);
        @SpiderPost("/users") UserDTO createUser(@Body CreateUserRequest req);
        @SpiderPut("/users/{id}") UserDTO updateUser(@Path("id") Long id, @Body CreateUserRequest req);
        @SpiderDelete("/users/{id}") void deleteUser(@Path("id") Long id);
    }

    @SpiderClient(name = "unreliable-service", url = "http://localhost:0", fallback = UnreliableFallback.class)
    public interface UnreliableClient {
        @SpiderGet("/flaky") @Retry(maxAttempts = 3, backoffMillis = 100) String flaky();
        @SpiderGet("/always-fail") String alwaysFail();
    }

    @SpiderClient(name = "cb-service", url = "http://localhost:0")
    public interface CbClient {
        @SpiderGet("/cb-ping") String ping();
    }

    public static class UnreliableFallback implements UnreliableClient {
        @Override public String flaky() { return "fallback-data"; }
        @Override public String alwaysFail() { return "fallback-always-fail"; }
    }

    public static class UserDTO {
        public Long id; public String name; public int age;
        @Override public String toString() { return "UserDTO{id=" + id + ", name=" + name + ", age=" + age + "}"; }
    }
    public static class CreateUserRequest {
        public String name; public int age;
        public CreateUserRequest() {}
        public CreateUserRequest(String n, int a) { name = n; age = a; }
    }

    // ---- 内嵌 HTTP Server ----

    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(null);
        server.createContext("/users/1", ex -> json(ex, 200, "{\"id\":1,\"name\":\"zhangsan\",\"age\":28}"));
        server.createContext("/users/42", ex -> json(ex, 200, "{\"id\":42,\"name\":\"lisi\",\"age\":35}"));
        server.createContext("/users", ex -> {
            if ("POST".equals(ex.getRequestMethod())) {
                String b = readBody(ex);
                String name = b.contains("name") ? b.split("\"name\":\"")[1].split("\"")[0] : "unknown";
                json(ex, 201, "{\"id\":100,\"name\":\"" + name + "\",\"age\":25}");
            } else {
                json(ex, 200, "[{\"id\":1,\"name\":\"zhangsan\",\"age\":28}]");
            }
        });
        server.createContext("/flaky", ex -> {
            if (flakyCounter.incrementAndGet() < 3) json(ex, 503, "");
            else json(ex, 200, "\"retry-ok\"");
        });
        server.createContext("/always-fail", ex -> json(ex, 500, ""));
        server.createContext("/cb-ping", ex -> json(ex, 500, ""));
        server.start();
        port = server.getAddress().getPort();
    }

    static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            byte[] b = new byte[4096]; int n = is.read(b);
            return n > 0 ? new String(b, 0, n) : "";
        }
    }

    static void json(HttpExchange ex, int code, String body) throws IOException {
        byte[] b = body.getBytes();
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    // ---- 工厂辅助 ----

    static SpiderClientFactory.Builder builder() {
        return SpiderClientFactory.builder()
                .transport(new OkHttpSpiderTransport())
                .decoder(new JacksonSpiderDecoder())
                .encoder(new JacksonSpiderEncoder());
    }

    static <T> T client(Class<T> iface) {
        return builder().url("http://localhost:" + port).build().create(iface);
    }

    // ---- 主程序 ----

    public static void main(String[] args) throws Exception {
        System.out.println("============================================");
        System.out.println("  Spider Demo v0.1.0");
        System.out.println("============================================");
        startServer();
        System.out.println("Server port: " + port);

        System.out.println();
        System.out.println("[1/7] Declarative GET");
        UserClient uc = client(UserClient.class);
        System.out.println("  GET /users/1 -> " + uc.getUser(1L));

        System.out.println();
        System.out.println("[2/7] Declarative POST");
        System.out.println("  POST /users -> " + uc.createUser(new CreateUserRequest("wangwu", 25)));

        System.out.println();
        System.out.println("[3/7] PUT / DELETE");
        System.out.println("  PUT  /users/42 -> " + uc.updateUser(42L, new CreateUserRequest("zhaoliu", 30)));
        uc.deleteUser(1L);
        System.out.println("  DELETE /users/1 -> void");

        System.out.println();
        System.out.println("[4/7] Retry (503 x2, then 200)");
        UnreliableClient rc = client(UnreliableClient.class);
        System.out.println("  Result: " + rc.flaky() + " (calls: " + flakyCounter.get() + ")");

        System.out.println();
        System.out.println("[5/7] Fallback (always 500)");
        System.out.println("  Result: " + rc.alwaysFail());

        System.out.println();
        System.out.println("[6/7] Circuit Breaker");
        CountingCircuitBreaker cb = new CountingCircuitBreaker(5, 10, 1000, 2);
        CbClient cc = SpiderClientFactory.builder()
                .transport(new OkHttpSpiderTransport())
                .decoder(new JacksonSpiderDecoder())
                .url("http://localhost:" + port)
                .circuitBreaker(cb).build().create(CbClient.class);
        for (int i = 0; i < 10; i++) { try { cc.ping(); } catch (Exception ignored) {} }
        System.out.println("  State: " + cb.state());
        try { cc.ping(); } catch (SpiderClientException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }

        System.out.println();
        System.out.println("[7/7] Interceptor");
        UserDTO u = SpiderClientFactory.builder()
                .transport(new OkHttpSpiderTransport())
                .decoder(new JacksonSpiderDecoder())
                .url("http://localhost:" + port)
                .addInterceptor(new SpiderInterceptor() {
                    @Override
                    public SpiderRequest beforeRequest(SpiderRequest req) {
                        req.addHeader("X-Demo", "v0.1.0");
                        return req;
                    }
                })
                .build().create(UserClient.class).getUser(1L);
        System.out.println("  Injected X-Demo header -> " + u);

        System.out.println();
        System.out.println("============================================");
        System.out.println("  7/7 PASS  Spider 0.1.0");
        System.out.println("============================================");
        server.stop(0);
    }
}
