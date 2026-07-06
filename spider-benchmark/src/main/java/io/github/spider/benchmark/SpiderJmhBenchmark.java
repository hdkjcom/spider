package io.github.spider.benchmark;

import com.sun.net.httpserver.HttpServer;
import io.github.spider.core.annotation.Path;
import io.github.spider.core.annotation.SpiderClient;
import io.github.spider.core.annotation.SpiderGet;
import io.github.spider.core.client.SpiderClientFactory;
import io.github.spider.http.OkHttpSpiderTransport;
import io.github.spider.jackson.JacksonSpiderDecoder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * JMH 规范微基准：Spider 声明式代理 vs 原生 OkHttp 的吞吐与延迟对比。
 *
 * <p>与 {@link SpiderBenchmark}（手写计时 main，快速粗略对比）互补，本类使用 JMH
 * 标准流程（fork / warmup / measurement）产出统计可靠的 QPS 与延迟数据。
 *
 * <p>运行前先编译以触发 JMH 注解处理器生成桩代码：
 * <pre>
 *   mvn -pl spider-benchmark compile
 *   mvn -pl spider-benchmark exec:java -Dexec.mainClass=io.github.spider.benchmark.SpiderJmhBenchmark
 * </pre>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class SpiderJmhBenchmark {

    private static final int PORT = 18081;

    @SpiderClient(name = "jmh", url = "http://localhost:18081")
    public interface BenchClient {
        @SpiderGet("/ping/{name}")
        String ping(@Path("name") String name);
    }

    private HttpServer server;
    private BenchClient spiderClient;
    private OkHttpClient rawOkHttp;

    @Setup
    public void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.createContext("/ping/", exchange -> {
            byte[] body = "\"ok\"".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        spiderClient = SpiderClientFactory.builder()
                .transport(new OkHttpSpiderTransport())
                .decoder(new JacksonSpiderDecoder())
                .build()
                .create(BenchClient.class);

        rawOkHttp = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @TearDown
    public void tearDown() {
        server.stop(0);
    }

    @Benchmark
    public String spiderProxyCall() {
        return spiderClient.ping("b");
    }

    @Benchmark
    public String rawOkHttpCall() throws IOException {
        try (Response r = rawOkHttp.newCall(new Request.Builder()
                .url("http://localhost:" + PORT + "/ping/b").build()).execute()) {
            return r.body().string();
        }
    }

    /** JMH 入口。 */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SpiderJmhBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
