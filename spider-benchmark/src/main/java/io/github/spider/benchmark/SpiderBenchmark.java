package io.github.spider.benchmark;

import com.sun.net.httpserver.HttpServer;
import io.github.spider.core.annotation.SpiderClient;
import io.github.spider.core.annotation.SpiderGet;
import io.github.spider.core.annotation.Path;
import io.github.spider.core.client.SpiderClientFactory;
import io.github.spider.http.OkHttpSpiderTransport;
import io.github.spider.jackson.JacksonSpiderDecoder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Performance benchmark: Spider vs raw OkHttp.
 *
 * Run via: {@code mvn exec:java -pl spider-benchmark}
 * or: {@code java -cp ... io.github.spider.benchmark.SpiderBenchmark}
 */
public class SpiderBenchmark {

    private static final int WARMUP_ITERATIONS = 5_000;
    private static final int MEASURE_ITERATIONS = 20_000;
    private static final int PORT = 18080;

    // --- Test client ---

    @SpiderClient(name = "bench", url = "http://localhost:18080")
    interface BenchClient {
        @SpiderGet("/ping/{name}")
        String ping(@Path("name") String name);
    }

    // --- Main ---

    public static void main(String[] args) throws Exception {
        // Start local HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
        server.createContext("/ping/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String name = path.substring(path.lastIndexOf('/') + 1);
            byte[] body = ("\"Hello, " + name + "!\"").getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });
        server.start();
        System.out.println("Server started on port " + PORT);
        System.out.println();

        // --- Prepare clients ---
        OkHttpClient rawOkHttp = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        BenchClient spiderClient = SpiderClientFactory.builder()
                .transport(new OkHttpSpiderTransport())
                .decoder(new JacksonSpiderDecoder())
                .build()
                .create(BenchClient.class);

        // --- Warm up ---
        System.out.println("Warming up (" + WARMUP_ITERATIONS + " iterations)...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            spiderClient.ping("warmup");
        }
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            try (Response r = rawOkHttp.newCall(new Request.Builder()
                    .url("http://localhost:" + PORT + "/ping/warmup").build()).execute()) {
                r.body().string();
            }
        }
        System.out.println("Warmup complete.");
        System.out.println();

        // --- Benchmark: Spider ---
        System.out.println("=== Spider Proxy (" + MEASURE_ITERATIONS + " iterations) ===");
        long spiderStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            spiderClient.ping("bench");
        }
        long spiderEnd = System.nanoTime();
        double spiderTotalMs = (spiderEnd - spiderStart) / 1_000_000.0;
        double spiderAvgUs = (spiderEnd - spiderStart) / 1_000.0 / MEASURE_ITERATIONS;
        double spiderQps = MEASURE_ITERATIONS / (spiderTotalMs / 1000.0);

        System.out.printf("  Total:   %.2f ms%n", spiderTotalMs);
        System.out.printf("  Avg:     %.2f µs/call%n", spiderAvgUs);
        System.out.printf("  QPS:     %.0f req/s%n", spiderQps);
        System.out.println();

        // --- Benchmark: Raw OkHttp ---
        System.out.println("=== Raw OkHttp (" + MEASURE_ITERATIONS + " iterations) ===");
        long okStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            try (Response r = rawOkHttp.newCall(new Request.Builder()
                    .url("http://localhost:" + PORT + "/ping/bench").build()).execute()) {
                r.body().string();
            }
        }
        long okEnd = System.nanoTime();
        double okTotalMs = (okEnd - okStart) / 1_000_000.0;
        double okAvgUs = (okEnd - okStart) / 1_000.0 / MEASURE_ITERATIONS;
        double okQps = MEASURE_ITERATIONS / (okTotalMs / 1000.0);

        System.out.printf("  Total:   %.2f ms%n", okTotalMs);
        System.out.printf("  Avg:     %.2f µs/call%n", okAvgUs);
        System.out.printf("  QPS:     %.0f req/s%n", okQps);
        System.out.println();

        // --- Summary ---
        System.out.println("=== Comparison ===");
        double overheadUs = spiderAvgUs - okAvgUs;
        double overheadPct = (overheadUs / okAvgUs) * 100;
        System.out.printf("  Spider overhead: %.2f µs/call (+%.1f%%)%n", overheadUs, overheadPct);
        System.out.printf("  Spider QPS:      %.0f req/s%n", spiderQps);
        System.out.printf("  OkHttp QPS:      %.0f req/s%n", okQps);
        System.out.printf("  QPS ratio:       %.2f%%%n", (spiderQps / okQps) * 100);

        server.stop(0);
    }
}
