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

    enum State { CLOSED, OPEN, HALF_OPEN }

    /** 永不打开的熔断器。 */
    SpiderCircuitBreaker NOOP = new SpiderCircuitBreaker() {
        @Override public boolean isAllowed() { return true; }
        @Override public void recordSuccess() {}
        @Override public void recordFailure(Throwable t) {}
        @Override public State state() { return State.CLOSED; }
    };
}
