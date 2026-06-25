package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.policy.SpiderCircuitBreaker;
import io.github.spider.core.runtime.SpiderRuntime;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
public class SpiderHealthIndicator implements HealthIndicator {

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
