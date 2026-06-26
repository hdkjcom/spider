package io.github.spider.core.exception;

/**
 * 熔断器拒绝：熔断器处于 OPEN 状态，所有请求被快速失败。
 * 此类错误不应被重试（熔断器已处理该逻辑）。
 */
public class SpiderCircuitBreakerOpenException extends SpiderException {

    private final String circuitBreakerName;

    public SpiderCircuitBreakerOpenException(String circuitBreakerName) {
        super("Circuit breaker is OPEN for " + circuitBreakerName);
        this.circuitBreakerName = circuitBreakerName;
    }

    /** 返回触发该异常的熔断器名称。 */
    public String circuitBreakerName() {
        return circuitBreakerName;
    }

    @Override
    public ErrorCategory category() {
        return ErrorCategory.CIRCUIT_BREAKER;
    }
}
