package io.github.spider.core.policy;

/**
 * 熔断器抽象。
 *
 * <p>Spider 提供两种实现：
 * <ul>
 * <li>{@code CountingCircuitBreaker} — 内置实现，无额外依赖</li>
 * <li>{@code ResilienceCircuitBreaker} — 封装 Resilience4j，位于 spider-resilience 模块</li>
 * </ul>
 */
public interface SpiderCircuitBreaker {

    /** 当前是否允许通过调用。 */
    boolean isAllowed();

    /** 记录一次成功调用（递减失败计数器）。 */
    void recordSuccess();

    /** 记录一次失败调用（可能打开熔断器）。 */
    void recordFailure(Throwable throwable);

    /** 当前状态。 */
    State state();

    /**
     * 运行时重新配置熔断阈值，用于动态治理：配置中心变更后无需重建客户端即可调整策略。
     * 默认空实现以保持向后兼容，{@code CountingCircuitBreaker} 和 {@code ResilienceCircuitBreaker} 覆盖此方法。
     *
     * @param failureRateThreshold 失败率阈值（百分比，0-100）
     * @param slidingWindowSize 滑动窗口大小（样本数）
     * @param waitDurationInOpenStateMillis OPEN 状态冷却等待时间（毫秒）
     * @param permittedNumberOfCallsInHalfOpenState HALF_OPEN 状态允许的试探调用数
     */
    default void reconfigure(int failureRateThreshold, int slidingWindowSize,
                             long waitDurationInOpenStateMillis,
                             int permittedNumberOfCallsInHalfOpenState) {
        // 默认空实现：不支持运行时重配的熔断器（如 NOOP）安全忽略
    }

    enum State { CLOSED, OPEN, HALF_OPEN }

    /** 永不打开的熔断器。 */
    SpiderCircuitBreaker NOOP = new SpiderCircuitBreaker() {
        @Override public boolean isAllowed() { return true; }
        @Override public void recordSuccess() {}
        @Override public void recordFailure(Throwable t) {}
        @Override public State state() { return State.CLOSED; }
    };
}
