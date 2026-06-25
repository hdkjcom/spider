package io.github.spider.core.client;

import io.github.spider.core.policy.SpiderCircuitBreaker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple in-memory circuit breaker implementation.
 * Used as the default when @SpiderCircuitBreaker is present but no custom implementation is provided.
 * Thread-safe.
 */
public class CountingCircuitBreaker implements SpiderCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(CountingCircuitBreaker.class);

    private final int failureRateThreshold;
    private final int slidingWindowSize;
    private final long waitDurationInOpenStateMillis;
    private final int permittedNumberOfCallsInHalfOpenState;

    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicReference<State> currentState = new AtomicReference<>(State.CLOSED);
    private final AtomicLong openedAt = new AtomicLong(0);
    private final AtomicInteger halfOpenCalls = new AtomicInteger(0);

    public CountingCircuitBreaker(io.github.spider.core.annotation.SpiderCircuitBreaker annotation) {
        this(annotation.failureRateThreshold(),
             annotation.slidingWindowSize(),
             annotation.waitDurationInOpenStateMillis(),
             annotation.permittedNumberOfCallsInHalfOpenState());
    }

    /** Convenience constructor for programmatic use (without annotation). */
    public CountingCircuitBreaker(int failureRateThreshold, int slidingWindowSize,
                                   long waitDurationInOpenStateMillis, int permittedNumberOfCallsInHalfOpenState) {
        this.failureRateThreshold = failureRateThreshold;
        this.slidingWindowSize = slidingWindowSize;
        this.waitDurationInOpenStateMillis = waitDurationInOpenStateMillis;
        this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
    }

    @Override
    public boolean isAllowed() {
        State state = currentState.get();

        if (state == State.CLOSED) {
            return true;
        }

        if (state == State.OPEN) {
            long now = System.currentTimeMillis();
            if (now - openedAt.get() >= waitDurationInOpenStateMillis) {
                // Transition to HALF_OPEN
                currentState.compareAndSet(State.OPEN, State.HALF_OPEN);
                halfOpenCalls.set(1);  // Count this transition call
                successCount.set(0);
                failureCount.set(0);
                return true;
            }
            return false;
        }

        // HALF_OPEN: allow limited calls
        if (state == State.HALF_OPEN) {
            return halfOpenCalls.incrementAndGet() <= permittedNumberOfCallsInHalfOpenState;
        }

        return true;
    }

    @Override
    public void recordSuccess() {
        int total = successCount.incrementAndGet() + failureCount.get();
        if (total >= slidingWindowSize) {
            resetWindow();
        }
        if (currentState.get() == State.HALF_OPEN && successCount.get() >= permittedNumberOfCallsInHalfOpenState) {
            currentState.set(State.CLOSED);
            successCount.set(0);
            failureCount.set(0);
        }
    }

    @Override
    public void recordFailure(Throwable throwable) {
        int failures = failureCount.incrementAndGet();
        int total = failures + successCount.get();

        // Check failure rate
        if (total >= slidingWindowSize) {
            double failureRate = (double) failures / total * 100;
            if (failureRate >= failureRateThreshold) {
                if (currentState.compareAndSet(State.CLOSED, State.OPEN)
                        || currentState.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                    openedAt.set(System.currentTimeMillis());
                    log.warn("CircuitBreaker OPEN (failureRate={}%, threshold={}%)", String.format("%.1f", failureRate), failureRateThreshold);
                }
            }
            resetWindow();
        }

        // In HALF_OPEN, a single failure re-opens the circuit
        if (currentState.get() == State.HALF_OPEN) {
            currentState.set(State.OPEN);
            openedAt.set(System.currentTimeMillis());
        }
    }

    @Override
    public State state() {
        return currentState.get();
    }

    private void resetWindow() {
        successCount.set(0);
        failureCount.set(0);
    }
}
