package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.runtime.SpiderRuntime;
import io.github.spider.core.runtime.dto.FullReportDto;
import io.github.spider.core.runtime.dto.RuntimeSummaryDto;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

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
    public RuntimeSummaryDto status() {
        return SpiderRuntime.getInstance().summary();
    }

    /**
     * 返回所有客户端的运行时详情（调用次数、成功率、延迟百分位等）。
     */
    @ReadOperation
    public FullReportDto clients() {
        return SpiderRuntime.getInstance().fullReport();
    }

    /**
     * 返回单个客户端的运行时详情。
     *
     * @param name 客户端名称（{@code @SpiderClient.name()}）
     */
    @ReadOperation
    public ClientDetailDto client(@Selector String name) {
        ClientDetailDto dto = new ClientDetailDto();
        SpiderRuntime.ClientStats stats = SpiderRuntime.getInstance().stats(name);
        if (stats == null) {
            dto.setError("Client not found: " + name);
            return dto;
        }
        dto.setName(stats.clientName);
        dto.setCalls(stats.callCount.get());
        dto.setSuccess(stats.successCount.get());
        dto.setFailure(stats.failureCount.get());
        dto.setRetries(stats.retryCount.get());
        dto.setFallbacks(stats.fallbackCount.get());
        dto.setAvgLatencyMs(String.format("%.2f", stats.avgLatencyMs()));
        dto.setP50(stats.latencyPercentile(50));
        dto.setP90(stats.latencyPercentile(90));
        dto.setP99(stats.latencyPercentile(99));
        long c = stats.callCount.get(), s = stats.successCount.get();
        dto.setSuccessRate(c > 0 ? String.format("%.1f", 100.0 * s / c) : "N/A");
        dto.setCurrentQps(stats.currentQps());
        return dto;
    }
}
