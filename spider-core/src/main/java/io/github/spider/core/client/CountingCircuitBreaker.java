package io.github.spider.core.client;

import io.github.spider.core.policy.SpiderCircuitBreaker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 简单的内存熔断器实现（计数窗口）。
 * 当存在 @SpiderCircuitBreaker 注解但未提供自定义实现时，作为默认实现使用。
 *
 * <p><b>近似窗口</b>：采用计数 + reset 模型（窗口满即清零重新计数），非严格的滑动窗口。
 * recordSuccess/recordFailure 的窗口评估与 reset 由 windowLock 串行保护，保证计数一致性。
 * 生产严肃场景或需要精确滑动窗口时，建议使用 spider-resilience 模块的
 * ResilienceCircuitBreaker（基于 Resilience4j，工业级滑动窗口实现）。
 */
public class CountingCircuitBreaker implements SpiderCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(CountingCircuitBreaker.class);

    // 阈值参数声明为 volatile：支持运行时 reconfigure() 动态刷新，保证多线程可见性。
    private volatile int failureRateThreshold;
    private volatile int slidingWindowSize;
    private volatile long waitDurationInOpenStateMillis;
    private volatile int permittedNumberOfCallsInHalfOpenState;

    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicReference<State> currentState = new AtomicReference<>(State.CLOSED);
    private final AtomicLong openedAt = new AtomicLong(0);
    private final AtomicInteger halfOpenCalls = new AtomicInteger(0);
    private final Object halfOpenLock = new Object();
    /** 保护 recordSuccess/recordFailure 的窗口评估与 reset 复合操作，避免并发下丢样本/失败率错算。 */
    private final Object windowLock = new Object();

    public CountingCircuitBreaker(io.github.spider.core.annotation.SpiderCircuitBreaker annotation) {
        this(annotation.failureRateThreshold(),
             annotation.slidingWindowSize(),
             annotation.waitDurationInOpenStateMillis(),
             annotation.permittedNumberOfCallsInHalfOpenState());
    }

    /** 便捷构造器，用于编程方式使用（无需注解）。 */
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
                synchronized (halfOpenLock) {
                    if (currentState.get() == State.OPEN
                            && now - openedAt.get() >= waitDurationInOpenStateMillis) {
                        currentState.set(State.HALF_OPEN);
                        successCount.set(0);
                        failureCount.set(0);
                        halfOpenCalls.set(1);
                        return true;
                    }
                }
            }
            return false;
        }

        // 半开状态：允许有限调用
        if (state == State.HALF_OPEN) {
            return halfOpenCalls.incrementAndGet() <= permittedNumberOfCallsInHalfOpenState;
        }

        return true;
    }

    @Override
    public void recordSuccess() {
        synchronized (windowLock) {
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
    }

    @Override
    public void recordFailure(Throwable throwable) {
        synchronized (windowLock) {
            int failures = failureCount.incrementAndGet();
            int total = failures + successCount.get();

            // 检查失败率
            if (total >= slidingWindowSize) {
                double failureRate = (double) failures / total * 100;
                if (failureRate >= failureRateThreshold) {
                    if (currentState.compareAndSet(State.CLOSED, State.OPEN)) {
                        openedAt.set(System.currentTimeMillis());
                        log.warn("熔断器打开 (失败率{}%, 阈值{}%)", String.format("%.1f", failureRate), failureRateThreshold);
                    }
                }
                resetWindow();
            }

            // 半开状态单次失败重新打开熔断器
            if (currentState.get() == State.HALF_OPEN) {
                currentState.set(State.OPEN);
                openedAt.set(System.currentTimeMillis());
            }
        }
    }

    @Override
    public State state() {
        return currentState.get();
    }

    @Override
    public void reconfigure(int failureRateThreshold, int slidingWindowSize,
                            long waitDurationInOpenStateMillis,
                            int permittedNumberOfCallsInHalfOpenState) {
        this.failureRateThreshold = failureRateThreshold;
        this.slidingWindowSize = slidingWindowSize;
        this.waitDurationInOpenStateMillis = waitDurationInOpenStateMillis;
        this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
    }

    private void resetWindow() {
        successCount.set(0);
        failureCount.set(0);
    }
}
