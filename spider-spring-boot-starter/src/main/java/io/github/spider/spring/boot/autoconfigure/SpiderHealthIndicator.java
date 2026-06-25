package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.policy.SpiderCircuitBreaker;
import io.github.spider.core.runtime.SpiderRuntime;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Spider 健康指示器，通过 {@code /actuator/health} 暴露 Spider 熔断器状态。
 * 当任一熔断器处于 OPEN 状态时，整体健康状态为 DOWN。
 */
@Component
@ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
public class SpiderHealthIndicator implements HealthIndicator {

    /**
     * 检查所有已注册熔断器的状态。
     *
     * @return 健康状态，包含各熔断器当前状态详情
     */
    @Override
    public Health health() {
        Map<String, SpiderCircuitBreaker.State> states = SpiderRuntime.getInstance().circuitBreakerStates();
        if (states.isEmpty()) return Health.up().withDetail("circuitBreakers", 0).build();

        Health.Builder builder = Health.up();
        for (Map.Entry<String, SpiderCircuitBreaker.State> e : states.entrySet()) {
            if (e.getValue() == SpiderCircuitBreaker.State.OPEN) {
                builder = Health.down().withDetail("openCircuit", e.getKey());
                break;
            }
        }
        return builder.withDetail("circuitBreakers", states).build();
    }
}
