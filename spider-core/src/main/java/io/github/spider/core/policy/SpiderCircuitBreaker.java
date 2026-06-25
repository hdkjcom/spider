package io.github.spider.core.policy;

/**
 * Circuit breaker abstraction.
 *
 * <p>Spider provides two implementations:
 * <ul>
 * <li>{@code CountingCircuitBreaker} — built-in, no extra dependency</li>
 * <li>{@code ResilienceCircuitBreaker} — wraps Resilience4j, in spider-resilience module</li>
 * </ul>
 */
public interface SpiderCircuitBreaker {

    /** Whether the circuit is currently allowing calls. */
    boolean isAllowed();

    /** Record a successful call (decrements failure counter). */
    void recordSuccess();

    /** Record a failed call (may open the circuit). */
    void recordFailure(Throwable throwable);

    /** Current state. */
    State state();

    enum State { CLOSED, OPEN, HALF_OPEN }

    /** Circuit breaker that never opens. */
    SpiderCircuitBreaker NOOP = new SpiderCircuitBreaker() {
        @Override public boolean isAllowed() { return true; }
        @Override public void recordSuccess() {}
        @Override public void recordFailure(Throwable t) {}
        @Override public State state() { return State.CLOSED; }
    };
}
