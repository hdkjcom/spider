package io.github.spider.resilience;

import io.github.spider.core.policy.SpiderCircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Resilience4j 的断路器实现。
 * 将核心断路策略委托给 Resilience4j 的 {@link CircuitBreaker}。
 *
 * @author Spider Team
 */
public class ResilienceCircuitBreaker implements SpiderCircuitBreaker {

    private final CircuitBreaker delegate;

    /**
     * 根据给定的名称和 {@code @SpiderCircuitBreaker} 注解配置创建断路器。
     *
     * @param name       断路器名称，用于在注册中心中标识
     * @param annotation 断路器注解，包含失败率阈值、滑动窗口大小、开状态等待时间等配置
     */
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

    /**
     * 使用已有的 Resilience4j {@link CircuitBreaker} 实例创建断路器。
     *
     * @param delegate 已有的 Resilience4j CircuitBreaker 实例
     */
    public ResilienceCircuitBreaker(CircuitBreaker delegate) {
        this.delegate = delegate;
    }

    /**
     * 判断当前请求是否允许通过断路器。
     *
     * @return true 表示允许通过，false 表示被断路器拦截
     */
    @Override
    public boolean isAllowed() {
        return delegate.tryAcquirePermission();
    }

    /**
     * 记录一次成功的调用，用于断路器的滑动窗口统计。
     */
    @Override
    public void recordSuccess() {
        delegate.onSuccess(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 记录一次失败的调用，用于断路器的滑动窗口统计。
     *
     * @param throwable 调用失败的异常
     */
    @Override
    public void recordFailure(Throwable throwable) {
        delegate.onError(System.currentTimeMillis(), TimeUnit.MILLISECONDS, throwable);
    }

    /**
     * 获取断路器当前状态。
     *
     * @return CLOSED（正常）、OPEN（熔断）或 HALF_OPEN（半开）
     */
    @Override
    public State state() {
        switch (delegate.getState()) {
            case CLOSED: return State.CLOSED;
            case OPEN: return State.OPEN;
            case HALF_OPEN: return State.HALF_OPEN;
            default: return State.CLOSED;
        }
    }

    /**
     * 暴露底层 Resilience4j {@link CircuitBreaker} 实例，供高级用户直接使用。
     *
     * @return 底层 Resilience4j CircuitBreaker 实例
     */
    public CircuitBreaker delegate() {
        return delegate;
    }
}
