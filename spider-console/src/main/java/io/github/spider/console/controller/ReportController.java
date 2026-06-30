package io.github.spider.console.controller;

import io.github.spider.console.dto.*;
import io.github.spider.core.runtime.SpiderRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private final Map<String, Map<String, Map<String, Object>>> store = new ConcurrentHashMap<>();
    private final Map<String, String> circuitBreakers = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> recentReports = Collections.synchronizedList(new ArrayList<>());

    /** 嵌入式模式下的服务名，默认取 spring.application.name */
    @Value("${spider.console.service-name:${spring.application.name:unknown}}")
    private String localServiceName;

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
            synchronized (recentReports) {
                recentReports.add(new LinkedHashMap<>(metricMap));
                while (recentReports.size() > 1000) recentReports.remove(0);
            }
        }

        for (Map.Entry<String, String> e : breakers.entrySet()) {
            circuitBreakers.put(e.getKey(), e.getValue());
        }

        ReportResult result = new ReportResult();
        result.setSaved(metrics.size());
        return result;
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        DashboardDto dto = new DashboardDto();
        Set<String> services = new LinkedHashSet<>(store.keySet());

        // 1. 远程上报数据
        for (Map.Entry<String, Map<String, Map<String, Object>>> se : store.entrySet()) {
            for (Map.Entry<String, Map<String, Object>> ce : se.getValue().entrySet()) {
                Map<String, Object> v = ce.getValue();
                ClientSummary cs = buildSummary(se.getKey(), v);
                dto.getClients().put(se.getKey() + "/" + ce.getKey(), cs);
            }
        }

        // 2. 本地 SpiderRuntime 数据，按方法拆分
        String service = localServiceName;
        for (String clientName : SpiderRuntime.getInstance().clientNames()) {
            Map<String, SpiderRuntime.ClientStats> methods = SpiderRuntime.getInstance().methodStats(clientName);
            if (methods == null) continue;
            for (Map.Entry<String, SpiderRuntime.ClientStats> me : methods.entrySet()) {
                String methodName = me.getKey();
                SpiderRuntime.ClientStats stats = me.getValue();
                String key = service + "/" + clientName + "#" + methodName;
                if (!dto.getClients().containsKey(key)) {
                    ClientSummary cs = new ClientSummary();
                    cs.setService(service);
                    cs.setClient(clientName);
                    cs.setMethod(methodName);
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
                    cs.setOutcomes(outcomeCompact(stats));
                    dto.getClients().put(key, cs);
                }
            }
            services.add(service);
        }

        dto.setServices(new ArrayList<>(services));

        // 3. 熔断器状态
        Map<String, String> cbStates = new LinkedHashMap<>(circuitBreakers);
        Map<String, io.github.spider.core.policy.SpiderCircuitBreaker.State> runtimeCb =
                SpiderRuntime.getInstance().circuitBreakerStates();
        for (Map.Entry<String, io.github.spider.core.policy.SpiderCircuitBreaker.State> e : runtimeCb.entrySet()) {
            cbStates.putIfAbsent(e.getKey(), e.getValue().name());
        }
        dto.setCircuitBreakers(cbStates);

        // 4. 最近上报
        if (!recentReports.isEmpty()) {
            dto.setSnapshotCount(recentReports.size());
            synchronized (recentReports) {
                int from = Math.max(0, recentReports.size() - 50);
                List<Map<String, Object>> recent = new ArrayList<>(recentReports.subList(from, recentReports.size()));
                Collections.reverse(recent);
                dto.setRecentReports(recent);
            }
        } else if (!services.isEmpty()) {
            List<Map<String, Object>> snapshots = new ArrayList<>();
            for (String clientName : SpiderRuntime.getInstance().clientNames()) {
                Map<String, SpiderRuntime.ClientStats> methods = SpiderRuntime.getInstance().methodStats(clientName);
                if (methods == null) continue;
                for (Map.Entry<String, SpiderRuntime.ClientStats> me : methods.entrySet()) {
                    SpiderRuntime.ClientStats s = me.getValue();
                    Map<String, Object> snap = new LinkedHashMap<>();
                    snap.put("service", localServiceName);
                    snap.put("client", clientName);
                    snap.put("method", me.getKey());
                    snap.put("calls", s.callCount.get());
                    snap.put("success", s.successCount.get());
                    snap.put("failure", s.failureCount.get());
                    snap.put("retries", s.retryCount.get());
                    snap.put("fallbacks", s.fallbackCount.get());
                    snap.put("p99", s.latencyPercentile(99));
                    long c = s.callCount.get();
                    snap.put("successRate", c > 0 ? String.format("%.1f%%", 100.0 * s.successCount.get() / c) : "N/A");
                    snap.put("reportTime", new Date());
                    snapshots.add(snap);
                }
            }
            dto.setSnapshotCount(snapshots.size());
            dto.setRecentReports(snapshots);
        } else {
            dto.setSnapshotCount(0);
            dto.setRecentReports(Collections.emptyList());
        }
        dto.setTime(new Date());
        return dto;
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

    private long toLong(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try { return Long.parseLong((String) v); }
            catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    /** 将最近 30 次调用结果转为紧凑字符串 "10111"，用于前端迷你趋势图。 */
    private static String outcomeCompact(SpiderRuntime.ClientStats stats) {
        if (stats == null) return "";
        java.util.List<io.github.spider.core.runtime.dto.OutcomePointDto> history = stats.outcomeHistory();
        if (history == null || history.isEmpty()) return "";
        int from = Math.max(0, history.size() - 30);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < history.size(); i++) {
            sb.append(history.get(i).isSuccess() ? '1' : '0');
        }
        return sb.toString();
    }
}
