package io.github.spider.console.controller;

import io.github.spider.console.dto.*;
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
    private final List<Map<String, Object>> recentReports = Collections.synchronizedList(new ArrayList<>());
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
            Map<String, Object> metricMap = new LinkedHashMap<>();
            metricMap.put("client", client);
            metricMap.put("calls", m.getCalls());
            metricMap.put("success", m.getSuccess());
            metricMap.put("failure", m.getFailure());
            metricMap.put("retries", m.getRetries());
            metricMap.put("fallbacks", m.getFallbacks());
            metricMap.put("totalLatencyMs", m.getTotalLatencyMs());
            metricMap.put("p50", m.getP50());
            metricMap.put("p90", m.getP90());
            metricMap.put("p99", m.getP99());
            serviceStore.put(client, metricMap);
            recentReports.add(metricMap);
            synchronized (recentReports) {
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
     */
    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        DashboardDto dto = new DashboardDto();
        dto.setServices(new ArrayList<>(store.keySet()));

        for (Map.Entry<String, Map<String, Map<String, Object>>> se : store.entrySet()) {
            for (Map.Entry<String, Map<String, Object>> ce : se.getValue().entrySet()) {
                Map<String, Object> v = ce.getValue();
                ClientSummary cs = new ClientSummary();
                cs.setService(se.getKey());
                cs.setClient(ce.getKey());
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
                dto.getClients().put(se.getKey() + "/" + ce.getKey(), cs);
            }
        }

        dto.setCircuitBreakers(new LinkedHashMap<>(circuitBreakers));
        dto.setSnapshotCount(recentReports.size());
        dto.setTracingEnabled(tracingEnabled);
        dto.setTime(new Date());
        return dto;
    }

    /** 容错类型转换：Number 取 longValue，String 解析为 long，其他返回 0 */
    private long toLong(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) return Long.parseLong((String) v);
        return 0;
    }
}
