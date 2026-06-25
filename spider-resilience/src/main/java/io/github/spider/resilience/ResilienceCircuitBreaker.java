package io.github.spider.resilience;

import io.github.spider.core.policy.SpiderCircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.time.Duration;

/**
 * Resilience4j-based SpiderCircuitBreaker implementation.
 * Delegates to Resilience4j's {@link CircuitBreaker}.
 */
public class ResilienceCircuitBreaker implements SpiderCircuitBreaker {

    private final CircuitBreaker delegate;

    public ResilienceCircuitBreaker(String name, io.github.spider.core.annotation.SpiderCircuitBreaker annotation) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(annotation.failureRateThreshold())
                .slidingWindowSize(annotation.slidingWindowSize())
                .waitDurationInOpenState(Duration.ofMillis(annotation.waitDurationInOpenStateMillis()))
                .permittedNumberOfCallsInHalfOpenState(annotation.permittedNumberOfCallsInHalfOpenState())
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        this.delegate = registry.circuitBreaker(name);
    }

    public ResilienceCircuitBreaker(CircuitBreaker delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isAllowed() {
        return delegate.tryAcquirePermission();
    }

    @Override
    public void recordSuccess() {
        delegate.onSuccess(System.currentTimeMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordFailure(Throwable throwable) {
        delegate.onError(System.currentTimeMillis(), java.util.concurrent.TimeUnit.MILLISECONDS, throwable);
    }

    @Override
    public State state() {
        switch (delegate.getState()) {
            case CLOSED: return State.CLOSED;
            case OPEN: return State.OPEN;
            case HALF_OPEN: return State.HALF_OPEN;
            default: return State.CLOSED;
        }
    }

    /** Expose the underlying Resilience4j CircuitBreaker for advanced usage. */
    public CircuitBreaker delegate() {
        return delegate;
    }
}
