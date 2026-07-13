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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("SpiderReporter 关闭中...");
            stop();
        }, "spider-reporter-shutdown"));

        int interval = Integer.parseInt(System.getProperty("spider.console.interval", "10"));

        executor.scheduleAtFixedRate(() -> {
            try {
                report(consoleUrl, serviceName);
            } catch (Exception e) {
                log.warn("上报失败", e);
            }
        }, 5, interval, TimeUnit.SECONDS);
    }

    /** 停止上报调度器，等待进行中的任务完成。多次调用安全（幂等）。 */
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
        payload.put("timestamp", System.currentTimeMillis());

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

    /**
     * 将 payload Map 序列化为 JSON 字符串。
     * 通用迭代 Map 结构，不硬编码 key 名。
     */
    static String buildJson(Map<String, Object> payload) {
        StringBuilder sb = new StringBuilder(256);
        writeMap(sb, payload);
        return sb.toString();
    }

    /** 序列化 Map 为 JSON object {@code {...}}。 */
    static void writeMap(StringBuilder sb, Map<String, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, ?> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(esc(e.getKey())).append("\":");
            writeValue(sb, e.getValue());
        }
        sb.append('}');
    }

    /** 按类型分发序列化。 */
    @SuppressWarnings("unchecked")
    static void writeValue(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof Boolean) {
            sb.append(((Boolean) v).booleanValue() ? "true" : "false");
        } else if (v instanceof Number) {
            sb.append(v.toString());
        } else if (v instanceof Map) {
            writeMap(sb, (Map<String, ?>) v);
        } else if (v instanceof List) {
            writeList(sb, (List<?>) v);
        } else {
            sb.append('"').append(esc(String.valueOf(v))).append('"');
        }
    }

    /** 序列化 List 为 JSON array {@code [...]}。 */
    static void writeList(StringBuilder sb, List<?> list) {
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            writeValue(sb, list.get(i));
        }
        sb.append(']');
    }

    /**
     * 对 JSON 字符串值进行转义。
     * 处理双引号、反斜杠、控制字符（含 \\b \\f \\n \\r \\t 及 U+0000–U+001F 的 \\u 形式）。
     */
    static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }
}
