package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.runtime.SpiderRuntime;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spider Actuator 端点，通过 {@code /actuator/spider} 暴露 Spider 运行时状态。
 *
 * <p>端点：
 * <ul>
 *   <li>{@code GET /actuator/spider} — 运行时摘要</li>
 *   <li>{@code GET /actuator/spider/clients} — 所有客户端详情</li>
 *   <li>{@code GET /actuator/spider/clients/{name}} — 单个客户端详情</li>
 * </ul>
 *
 * <p>仅在 classpath 中包含 Spring Boot Actuator 时生效。</p>
 */
@Component
@Endpoint(id = "spider")
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
public class SpiderActuatorEndpoint {

    /**
     * 返回 Spider 运行时汇总信息。
     */
    @ReadOperation
    public Map<String, Object> status() {
        return SpiderRuntime.getInstance().summary();
    }

    /**
     * 返回所有客户端的运行时详情（调用次数、成功率、延迟百分位等）。
     */
    @ReadOperation
    public Map<String, Object> clients() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clients", SpiderRuntime.getInstance().fullReport().get("clients"));
        result.put("circuitBreakers", SpiderRuntime.getInstance().circuitBreakerStates());
        result.put("recentErrors", SpiderRuntime.getInstance().recentErrors());
        return result;
    }

    /**
     * 返回单个客户端的运行时详情。
     *
     * @param name 客户端名称（{@code @SpiderClient.name()}）
     */
    @ReadOperation
    public Map<String, Object> client(@Selector String name) {
        Map<String, Object> result = new LinkedHashMap<>();
        SpiderRuntime.ClientStats stats = SpiderRuntime.getInstance().stats(name);
        if (stats == null) {
            result.put("error", "Client not found: " + name);
            return result;
        }
        result.put("name", stats.clientName);
        result.put("calls", stats.callCount.get());
        result.put("success", stats.successCount.get());
        result.put("failure", stats.failureCount.get());
        result.put("retries", stats.retryCount.get());
        result.put("fallbacks", stats.fallbackCount.get());
        result.put("avgLatencyMs", String.format("%.2f", stats.avgLatencyMs()));
        result.put("p50", stats.latencyPercentile(50));
        result.put("p90", stats.latencyPercentile(90));
        result.put("p99", stats.latencyPercentile(99));
        long c = stats.callCount.get(), s = stats.successCount.get();
        result.put("successRate", c > 0 ? String.format("%.1f", 100.0 * s / c) : "N/A");
        result.put("currentQps", stats.currentQps());
        return result;
    }
}
