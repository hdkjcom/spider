package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.policy.SpiderCircuitBreaker;
import io.github.spider.core.runtime.SpiderRuntime;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spider 健康指示器，通过 {@code /actuator/health} 暴露 Spider 运行时健康状态。
 *
 * <p>当任一熔断器处于 OPEN 状态时，整体健康状态为 DOWN。
 * 同时报告每个客户端的关键指标（调用次数、成功率、平均延迟）。
 */
@Component
@ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
public class SpiderHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        Map<String, SpiderCircuitBreaker.State> cbStates = SpiderRuntime.getInstance().circuitBreakerStates();

        Health.Builder builder = Health.up();

        // 熔断器状态
        builder.withDetail("circuitBreakers", cbStates.isEmpty() ? 0 : cbStates);
        for (Map.Entry<String, SpiderCircuitBreaker.State> e : cbStates.entrySet()) {
            if (e.getValue() == SpiderCircuitBreaker.State.OPEN) {
                builder = Health.down().withDetail("openCircuit", e.getKey());
                break;
            }
        }

        // 每个客户端的关键指标
        Map<String, Map<String, Object>> clientHealth = new LinkedHashMap<>();
        for (String name : SpiderRuntime.getInstance().clientNames()) {
            SpiderRuntime.ClientStats stats = SpiderRuntime.getInstance().stats(name);
            if (stats == null) continue;
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("calls", stats.callCount.get());
            detail.put("successRate", stats.callCount.get() > 0
                    ? String.format("%.1f%%", 100.0 * stats.successCount.get() / stats.callCount.get())
                    : "N/A");
            detail.put("avgLatencyMs", String.format("%.1f", stats.avgLatencyMs()));
            detail.put("currentQps", stats.currentQps());
            clientHealth.put(name, detail);
        }
        builder.withDetail("clients", clientHealth);

        // 整体统计
        Map<String, Object> summary = SpiderRuntime.getInstance().summary();
        builder.withDetail("uptimeSeconds", summary.get("uptimeSeconds"));
        builder.withDetail("totalCalls", summary.get("totalCalls"));
        builder.withDetail("successRate", summary.get("successRate"));

        return builder.build();
    }
}
