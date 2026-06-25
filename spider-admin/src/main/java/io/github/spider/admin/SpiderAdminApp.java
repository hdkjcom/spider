package io.github.spider.admin;

import io.github.spider.core.annotation.*;
import io.github.spider.core.client.SpiderClientFactory;
import io.github.spider.core.client.SpiderResponseContext;
import io.github.spider.http.OkHttpSpiderTransport;
import io.github.spider.jackson.JacksonSpiderDecoder;
import io.github.spider.jackson.JacksonSpiderEncoder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class SpiderAdminApp {

    private static final Random RNG = new Random();
    private static final AtomicInteger flakyCounter = new AtomicInteger(0);
    private static com.sun.net.httpserver.HttpServer server;
    private static int port;

    // ---- real clients ----

    @SpiderClient(name = "user-service", url = "http://localhost:0")
    public interface UserClient {
        @SpiderGet("/api/users/{id}") @Timeout(2000) String getUser(@Path("id") Long id);
        @SpiderPost("/api/users") @Timeout(2000) String createUser(@Body String body);
    }

    @SpiderClient(name = "order-service", url = "http://localhost:0",
                  fallbackFactory = OrderFallbackFactory.class)
    public interface OrderClient {
        @SpiderGet("/api/orders/{id}") @Retry(maxAttempts = 3, backoffMillis = 100) String getOrder(@Path("id") Long id);
        @SpiderGet("/api/orders/flaky") @Retry(maxAttempts = 2) String flaky();
    }

    public static class OrderFallbackFactory implements io.github.spider.core.policy.FallbackFactory<OrderClient> {
        @Override public OrderClient create(Throwable cause) {
            return new OrderClient() {
                @Override public String getOrder(Long id) { return "fallback"; }
                @Override public String flaky() { return "fallback"; }
            };
        }
    }

    @SpiderClient(name = "pay-service", url = "http://localhost:0")
    @io.github.spider.core.annotation.SpiderCircuitBreaker(failureRateThreshold = 40, slidingWindowSize = 10)
    public interface PayClient {
        @SpiderGet("/api/pay/status") String status();
    }

    // ---- entry ----

    public static void main(String[] args) throws Exception {
        startServer();
        SpringApplication.run(SpiderAdminApp.class, args);
        startWorkload();
    }

    @Bean
    public SpiderAdminService spiderAdminService() { return new SpiderAdminService(); }

    // ---- embedded target server ----

    static void startServer() throws IOException {
        server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));

        server.createContext("/api/users/", ex -> {
            String path = ex.getRequestURI().getPath();
            long id = Long.parseLong(path.substring(path.lastIndexOf('/')+1));
            if (RNG.nextInt(100) < 5) { // 5% failure
                json(ex, 500, "{\"error\":\"db timeout\"}");
            } else {
                addLatency(10, 50);
                json(ex, 200, "{\"id\":"+id+",\"name\":\"user-"+id+"\"}");
            }
        });
        server.createContext("/api/users", ex -> {
            json(ex, 201, "{\"id\":200,\"name\":\"new-user\"}");
        });

        server.createContext("/api/orders/", ex -> {
            String path = ex.getRequestURI().getPath();
            if (path.contains("flaky")) {
                int c = flakyCounter.incrementAndGet();
                if (c % 5 == 0) { json(ex, 200, "{\"status\":\"ok\"}"); return; }
                if (c % 3 == 0) { json(ex, 503, "{\"error\":\"overload\"}"); return; }
                json(ex, 200, "{\"status\":\"ok\"}");
            } else {
                addLatency(20, 80);
                if (RNG.nextInt(100) < 10) {
                    json(ex, 500, "{\"error\":\"service unavailable\"}");
                } else {
                    json(ex, 200, "{\"status\":\"shipped\"}");
                }
            }
        });

        server.createContext("/api/pay/status", ex -> {
            if (RNG.nextInt(100) < 20) {
                json(ex, 500, "{\"error\":\"payment gateway timeout\"}");
            } else {
                addLatency(30, 120);
                json(ex, 200, "{\"status\":\"active\"}");
            }
        });

        server.start();
        port = server.getAddress().getPort();
    }

    static void addLatency(int minMs, int maxMs) {
        try { Thread.sleep(minMs + RNG.nextInt(maxMs - minMs)); } catch (InterruptedException ignored) {}
    }

    static void json(com.sun.net.httpserver.HttpExchange ex, int code, String body) throws IOException {
        byte[] b = body.getBytes();
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    // ---- workload generator ----

    static void startWorkload() {
        String base = "http://localhost:" + port;
        UserClient uc = SpiderClientFactory.builder()
                .transport(new OkHttpSpiderTransport()).decoder(new JacksonSpiderDecoder()).encoder(new JacksonSpiderEncoder())
                .url(base).build().create(UserClient.class);
        OrderClient oc = SpiderClientFactory.builder()
                .transport(new OkHttpSpiderTransport()).decoder(new JacksonSpiderDecoder())
                .url(base).build().create(OrderClient.class);
        PayClient pc = SpiderClientFactory.builder()
                .transport(new OkHttpSpiderTransport()).decoder(new JacksonSpiderDecoder())
                .url(base).build().create(PayClient.class);

        ScheduledExecutorService exec = Executors.newScheduledThreadPool(3);

        // user-service: 10 QPS
        exec.scheduleAtFixedRate(() -> {
            try { uc.getUser(RNG.nextLong() % 100 + 1); } catch (Exception ignored) {}
        }, 0, 100, TimeUnit.MILLISECONDS);

        // order-service: 5 QPS
        exec.scheduleAtFixedRate(() -> {
            try { oc.flaky(); } catch (Exception ignored) {}
            try { oc.getOrder(RNG.nextLong() % 50 + 1); } catch (Exception ignored) {}
        }, 0, 200, TimeUnit.MILLISECONDS);

        // pay-service: 3 QPS
        exec.scheduleAtFixedRate(() -> {
            try { pc.status(); } catch (Exception ignored) {}
        }, 0, 333, TimeUnit.MILLISECONDS);

        System.out.println("[Spider Admin] Workload started: user-service(10qps) order-service(5qps) pay-service(3qps)");
    }
}
