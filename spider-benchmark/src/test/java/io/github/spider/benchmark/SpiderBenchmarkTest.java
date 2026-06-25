package io.github.spider.benchmark;

import com.sun.net.httpserver.HttpServer;
import io.github.spider.core.client.SpiderClientFactory;
import io.github.spider.http.OkHttpSpiderTransport;
import io.github.spider.jackson.JacksonSpiderDecoder;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class SpiderBenchmarkTest {

    @Test
    void testBenchmarkRunsWithoutError() throws Exception {
        int port = 18081;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/ping/", exchange -> {
            byte[] body = "\"ok\"".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });
        server.start();

        try {
            SpiderBenchmark.BenchClient client = SpiderClientFactory.builder()
                    .transport(new OkHttpSpiderTransport())
                    .decoder(new JacksonSpiderDecoder())
                    .url("http://localhost:" + port)
                    .build()
                    .create(SpiderBenchmark.BenchClient.class);

            // Run a small number of iterations
            for (int i = 0; i < 100; i++) {
                String result = client.ping("test");
                assertNotNull(result);
            }
        } finally {
            server.stop(0);
        }
    }
}
