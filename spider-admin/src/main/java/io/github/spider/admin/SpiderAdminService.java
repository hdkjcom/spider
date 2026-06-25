package io.github.spider.admin;

import io.github.spider.core.runtime.SpiderRuntime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects Spider runtime state for admin/monitoring purposes.
 * Exposes health, status, client list, circuit breaker states, and metrics summary.
 */
public class SpiderAdminService {

    /**
     * Health check — returns "UP" if Spider is operational.
     */
    public Map<String, Object> health() {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("status", "UP");
        h.put("clients", SpiderRuntime.getInstance().clientNames().size());
        return h;
    }

    /**
     * Summary: uptime, total calls, success rate.
     */
    public Map<String, Object> summary() {
        return SpiderRuntime.getInstance().summary();
    }

    /**
     * List all registered clients with per-client stats.
     */
    public Map<String, Map<String, Object>> clients() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        SpiderRuntime runtime = SpiderRuntime.getInstance();
        for (String name : runtime.clientNames()) {
            SpiderRuntime.ClientStats stats = runtime.stats(name);
            if (stats == null) continue;
            Map<String, Object> cs = new LinkedHashMap<>();
            cs.put("name", stats.clientName);
            cs.put("calls", stats.callCount.get());
            cs.put("success", stats.successCount.get());
            cs.put("failure", stats.failureCount.get());
            cs.put("retries", stats.retryCount.get());
            cs.put("fallbacks", stats.fallbackCount.get());
            cs.put("avgLatencyMs", String.format("%.2f", stats.avgLatencyMs()));
            long c = stats.callCount.get();
            long s = stats.successCount.get();
            cs.put("successRate", c > 0 ? String.format("%.1f", 100.0 * s / c) : "100.0");
            result.put(name, cs);
        }
        return result;
    }

    /**
     * Circuit breaker states for all registered clients.
     */
    public Map<String, String> circuitBreakers() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, io.github.spider.core.policy.SpiderCircuitBreaker.State> e :
                SpiderRuntime.getInstance().circuitBreakerStates().entrySet()) {
            result.put(e.getKey(), e.getValue().name());
        }
        return result;
    }

    /**
     * Full dashboard snapshot: health + summary + clients + circuit breakers.
     */
    public Map<String, Object> dashboard() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("health", health());
        d.put("summary", summary());
        d.put("clients", clients());
        d.put("circuitBreakers", circuitBreakers());
        return d;
    }
}
