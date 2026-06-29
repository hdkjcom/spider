package io.github.spider.core.reporter;

import io.github.spider.core.runtime.SpiderRuntime;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定期向 Spider Console 上报 Spider 运行指标。
 *
 * <p>通过系统属性或 application.properties 配置：
 * <pre>
 * spider.console.url=http://localhost:18080
 * spider.console.interval=10
 * </pre>
 */
public class SpiderReporter {

    private static final Logger log = LoggerFactory.getLogger(SpiderReporter.class);

    private static final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "spider-reporter");
                t.setDaemon(true);
                return t;
            });

    private static final AtomicBoolean started = new AtomicBoolean(false);

    public static void start(String consoleUrl, String serviceName) {
        if (!started.compareAndSet(false, true)) return;
        int interval = Integer.parseInt(System.getProperty("spider.console.interval", "10"));

        executor.scheduleAtFixedRate(() -> {
            try {
                report(consoleUrl, serviceName);
            } catch (Exception e) {
                log.warn("上报失败", e);
            }
        }, 5, interval, TimeUnit.SECONDS);
    }

    /** 停止上报调度器，等待进行中的任务完成。 */
    public static void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static void report(String consoleUrl, String serviceName) throws Exception {
        SpiderRuntime rt = SpiderRuntime.getInstance();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("service", serviceName);
        payload.put("timestamp", new Date());

        List<Map<String, Object>> metrics = new ArrayList<>();
        for (String name : rt.clientNames()) {
            SpiderRuntime.ClientStats cs = rt.stats(name);
            if (cs == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("client", name);
            m.put("calls", cs.callCount.get());
            m.put("success", cs.successCount.get());
            m.put("failure", cs.failureCount.get());
            m.put("retries", cs.retryCount.get());
            m.put("fallbacks", cs.fallbackCount.get());
            m.put("totalLatencyMs", cs.totalLatencyMs.get());
            m.put("p50", cs.latencyPercentile(50));
            m.put("p90", cs.latencyPercentile(90));
            m.put("p99", cs.latencyPercentile(99));
            metrics.add(m);
        }
        payload.put("metrics", metrics);

        Map<String, Object> breakers = new LinkedHashMap<>();
        for (Map.Entry<String, io.github.spider.core.policy.SpiderCircuitBreaker.State> e :
                rt.circuitBreakerStates().entrySet()) {
            breakers.put(e.getKey(), e.getValue().name());
        }
        payload.put("circuitBreakers", breakers);

        boolean tracingAvailable;
        try {
            Class.forName("io.opentelemetry.api.OpenTelemetry");
            tracingAvailable = true;
        } catch (ClassNotFoundException e) {
            tracingAvailable = false;
        }
        Map<String, Object> tracing = new LinkedHashMap<>();
        tracing.put("enabled", tracingAvailable);
        payload.put("tracing", tracing);

        String json = buildJson(payload);
        URL url = new URL(consoleUrl + "/api/report");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            if (code >= 400) {
                log.warn("上报返回 HTTP {}", code);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static String buildJson(Map<String, Object> payload) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"service\":\"").append(esc(payload.get("service"))).append("\",");
        sb.append("\"metrics\":[");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metrics = (List<Map<String, Object>>) payload.get("metrics");
        if (metrics != null) {
            for (int i = 0; i < metrics.size(); i++) {
                if (i > 0) sb.append(",");
                Map<String, Object> m = metrics.get(i);
                sb.append("{");
                boolean first = true;
                for (Map.Entry<String, Object> e : m.entrySet()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("\"").append(esc(e.getKey())).append("\":").append(val(e.getValue()));
                }
                sb.append("}");
            }
        }
        sb.append("],\"circuitBreakers\":{");
        @SuppressWarnings("unchecked")
        Map<String, Object> breakers = (Map<String, Object>) payload.get("circuitBreakers");
        if (breakers != null) {
            boolean first = true;
            for (Map.Entry<String, Object> e : breakers.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(esc(e.getKey())).append("\":\"").append(esc(e.getValue())).append("\"");
            }
        }
        sb.append("},\"tracing\":{");
        @SuppressWarnings("unchecked")
        Map<String, Object> tracing = (Map<String, Object>) payload.get("tracing");
        if (tracing != null) {
            sb.append("\"enabled\":").append(tracing.get("enabled"));
        }
        sb.append("}}");
        return sb.toString();
    }

    private static String val(Object v) {
        if (v instanceof Number) return v.toString();
        return "\"" + esc(String.valueOf(v)) + "\"";
    }

    private static String esc(Object s) { return String.valueOf(s).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"); }
}
