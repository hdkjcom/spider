package io.github.spider.console.controller;

import io.github.spider.console.dto.*;
import io.github.spider.core.runtime.SpiderRuntime;
import io.github.spider.core.runtime.dto.ErrorEntryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 指标上报与查询接口。
 */
@RestController
@RequestMapping("/spider/api")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    /** 内存存储：服务名 -> 客户端名 -> 指标数据 */
    private final Map<String, Map<String, Map<String, Object>>> store = new ConcurrentHashMap<>();
    /** 熔断器最新状态 */
    private final Map<String, String> circuitBreakers = new ConcurrentHashMap<>();
    /** 最近上报的指标快照，最多保留 1000 条 */
    private final List<ErrorEntryDto> recentReports = Collections.synchronizedList(new ArrayList<>());
    /** 链路追踪是否已启用 */
    private volatile boolean tracingEnabled = false;

    /**
     * 接收客户端上报的指标数据。
     */
    @PostMapping("/report")
    public ReportResult report(@RequestBody ReportPayload payload) {
        String service = payload.getService() != null ? payload.getService() : "unknown";
        List<MetricDto> metrics = payload.getMetrics() != null ? payload.getMetrics() : Collections.emptyList();
        Map<String, String> breakers = payload.getCircuitBreakers() != null ? payload.getCircuitBreakers() : Collections.emptyMap();

        log.info("收到上报请求，服务={}，条数={}", service, metrics.size());

        Map<String, Map<String, Object>> serviceStore = store.computeIfAbsent(service, k -> new ConcurrentHashMap<>());

        for (MetricDto m : metrics) {
            String client = m.getClient() != null ? m.getClient() : service;
            String method = m.getMethod() != null && !m.getMethod().trim().isEmpty() ? m.getMethod() : "*";
            Map<String, Object> metricMap = new LinkedHashMap<>();
            metricMap.put("service", service);
            metricMap.put("client", client);
            metricMap.put("method", method);
            metricMap.put("calls", m.getCalls());
            metricMap.put("success", m.getSuccess());
            metricMap.put("failure", m.getFailure());
            metricMap.put("retries", m.getRetries());
            metricMap.put("fallbacks", m.getFallbacks());
            metricMap.put("totalLatencyMs", m.getTotalLatencyMs());
            metricMap.put("p50", m.getP50());
            metricMap.put("p90", m.getP90());
            metricMap.put("p99", m.getP99());
            long calls = m.getCalls();
            metricMap.put("successRate", calls > 0 ? String.format("%.1f", 100.0 * m.getSuccess() / calls) : "N/A");
            metricMap.put("avgLatencyMs", calls > 0 ? String.format("%.1f", (double) m.getTotalLatencyMs() / calls) : "0");
            metricMap.put("reportTime", new Date());
            serviceStore.put(client + "#" + method, metricMap);
            long failures = m.getFailure();
            ErrorEntryDto entry = new ErrorEntryDto();
            entry.setClient(client);
            entry.setMethod(method);
            entry.setMessage("service=" + service + " calls=" + calls + " successRate=" + metricMap.get("successRate"));
            entry.setErrorType(failures > 0 ? "failure" : "success");
            entry.setTime(System.currentTimeMillis());
            synchronized (recentReports) {
                recentReports.add(entry);
                while (recentReports.size() > 1000) recentReports.remove(0);
            }
        }

        for (Map.Entry<String, String> e : breakers.entrySet()) {
            circuitBreakers.put(e.getKey(), e.getValue());
        }

        TracingDto tracing = payload.getTracing();
        if (tracing != null && tracing.isEnabled()) {
            tracingEnabled = true;
        }

        ReportResult result = new ReportResult();
        result.setSaved(metrics.size());
        return result;
    }

    /**
     * 查询控制台仪表盘数据。
     * 同时合并远程上报数据（store）和本地运行时数据（SpiderRuntime）。
     */
    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        DashboardDto dto = new DashboardDto();
        Set<String> services = new LinkedHashSet<>(store.keySet());

        // 1. 合并远程上报的 store 数据
        for (Map.Entry<String, Map<String, Map<String, Object>>> se : store.entrySet()) {
            for (Map.Entry<String, Map<String, Object>> ce : se.getValue().entrySet()) {
                Map<String, Object> v = ce.getValue();
                ClientSummary cs = buildSummary(se.getKey(), v);
                dto.getClients().put(se.getKey() + "/" + ce.getKey(), cs);
            }
        }

        // 2. 合并本地 SpiderRuntime 数据（嵌入式模式下 store 可能为空）
        Map<String, SpiderRuntime.ClientStats> runtimeStats = collectRuntimeClients();
        for (Map.Entry<String, SpiderRuntime.ClientStats> e : runtimeStats.entrySet()) {
            String clientName = e.getKey();
            SpiderRuntime.ClientStats stats = e.getValue();
            String service = "local";
            String key = service + "/" + clientName + "#*";
            if (!dto.getClients().containsKey(service + "/" + clientName + "#*")) {
                ClientSummary cs = new ClientSummary();
                cs.setService(service);
                cs.setClient(clientName);
                cs.setMethod("*");
                cs.setCalls(stats.callCount.get());
                cs.setSuccess(stats.successCount.get());
                cs.setFailure(stats.failureCount.get());
                cs.setRetries(stats.retryCount.get());
                cs.setFallbacks(stats.fallbackCount.get());
                cs.setTotalLatencyMs(stats.totalLatencyMs.get());
                cs.setP50(stats.latencyPercentile(50));
                cs.setP90(stats.latencyPercentile(90));
                cs.setP99(stats.latencyPercentile(99));
                long c = stats.callCount.get();
                cs.setSuccessRate(c > 0 ? String.format("%.1f", 100.0 * stats.successCount.get() / c) : "N/A");
                cs.setAvgLatencyMs(c > 0 ? String.format("%.1f", stats.avgLatencyMs()) : "0");
                dto.getClients().put(key, cs);
            }
            services.add(service);
        }

        dto.setServices(new ArrayList<>(services));

        // 3. 合并熔断器状态
        Map<String, String> cbStates = new LinkedHashMap<>(circuitBreakers);
        Map<String, io.github.spider.core.policy.SpiderCircuitBreaker.State> runtimeCb =
                SpiderRuntime.getInstance().circuitBreakerStates();
        for (Map.Entry<String, io.github.spider.core.policy.SpiderCircuitBreaker.State> e : runtimeCb.entrySet()) {
            cbStates.putIfAbsent(e.getKey(), e.getValue().name());
        }
        dto.setCircuitBreakers(cbStates);

        // 最近上报：远程为空时从 SpiderRuntime 生成快照
        if (!recentReports.isEmpty()) {
            dto.setSnapshotCount(recentReports.size());
            synchronized (recentReports) {
                int from = Math.max(0, recentReports.size() - 50);
                List<ErrorEntryDto> recent = new ArrayList<>(recentReports.subList(from, recentReports.size()));
                Collections.reverse(recent);
                dto.setRecentReports(recent);
            }
        } else if (!runtimeStats.isEmpty()) {
            // 嵌入式模式：从本地运行时获取最近错误
            List<ErrorEntryDto> errors = SpiderRuntime.getInstance().recentErrors();
            dto.setSnapshotCount(errors.size());
            dto.setRecentReports(errors);
        } else {
            dto.setSnapshotCount(0);
            dto.setRecentReports(Collections.emptyList());
        }
        // 嵌入式模式下追踪状态从未上报，检查 classpath 上是否有 OpenTelemetry
        boolean tracingActive = tracingEnabled || isOpenTelemetryPresent();
        dto.setTracingEnabled(tracingActive);
        dto.setTime(new Date());
        return dto;
    }

    /** 检查 classpath 上是否存在 OpenTelemetry Tracer。 */
    private static boolean isOpenTelemetryPresent() {
        try {
            Class.forName("io.opentelemetry.api.trace.Tracer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** 从 SpiderRuntime 收集所有客户端统计信息 */
    private Map<String, SpiderRuntime.ClientStats> collectRuntimeClients() {
        Map<String, SpiderRuntime.ClientStats> result = new LinkedHashMap<>();
        for (String name : SpiderRuntime.getInstance().clientNames()) {
            SpiderRuntime.ClientStats stats = SpiderRuntime.getInstance().stats(name);
            if (stats != null && stats.callCount.get() > 0) {
                result.put(name, stats);
            }
        }
        return result;
    }

    private ClientSummary buildSummary(String service, Map<String, Object> v) {
        ClientSummary cs = new ClientSummary();
        cs.setService(service);
        cs.setClient(String.valueOf(v.get("client")));
        cs.setMethod(String.valueOf(v.get("method")));
        cs.setCalls(toLong(v.get("calls")));
        cs.setSuccess(toLong(v.get("success")));
        cs.setFailure(toLong(v.get("failure")));
        cs.setRetries(toLong(v.get("retries")));
        cs.setFallbacks(toLong(v.get("fallbacks")));
        cs.setTotalLatencyMs(toLong(v.get("totalLatencyMs")));
        cs.setP50(toLong(v.get("p50")));
        cs.setP90(toLong(v.get("p90")));
        cs.setP99(toLong(v.get("p99")));
        long calls = toLong(v.get("calls"));
        long succ = toLong(v.get("success"));
        cs.setSuccessRate(calls > 0 ? String.format("%.1f", 100.0 * succ / calls) : "N/A");
        long lat = toLong(v.get("totalLatencyMs"));
        cs.setAvgLatencyMs(calls > 0 ? String.format("%.1f", (double) lat / calls) : "0");
        return cs;
    }

    /** 容错类型转换：Number 取 longValue，String 解析为 long，其他返回 0 */
    private long toLong(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) return Long.parseLong((String) v);
        return 0;
    }
}
