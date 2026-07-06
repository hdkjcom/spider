package io.github.spider.resilience;

import io.github.spider.core.policy.SpiderCircuitBreaker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResilienceCircuitBreakerTest {

    @io.github.spider.core.annotation.SpiderCircuitBreaker(
        failureRateThreshold = 50,
        slidingWindowSize = 10,
        waitDurationInOpenStateMillis = 200,
        permittedNumberOfCallsInHalfOpenState = 2
    )
    interface TestConfig {}

    private ResilienceCircuitBreaker create() throws Exception {
        io.github.spider.core.annotation.SpiderCircuitBreaker ann =
                TestConfig.class.getAnnotation(io.github.spider.core.annotation.SpiderCircuitBreaker.class);
        return new ResilienceCircuitBreaker("test-cb", ann);
    }

    @Test
    void testInitiallyClosed() throws Exception {
        ResilienceCircuitBreaker cb = create();
        assertEquals(SpiderCircuitBreaker.State.CLOSED, cb.state());
        assertTrue(cb.isAllowed());
    }

    @Test
    void testRecordsSuccessAndFailure() throws Exception {
        ResilienceCircuitBreaker cb = create();
        cb.recordSuccess();
        cb.recordFailure(new RuntimeException("error"));
        // Still closed — not enough failures to open
        assertEquals(SpiderCircuitBreaker.State.CLOSED, cb.state());
    }

    @Test
    void testOpensAfterFailures() throws Exception {
        ResilienceCircuitBreaker cb = create();
        // 10 failures will trigger open (slidingWindowSize=10, all failures=100% > 50%)
        for (int i = 0; i < 10; i++) {
            cb.recordFailure(new RuntimeException("error-" + i));
        }
        assertEquals(SpiderCircuitBreaker.State.OPEN, cb.state());
        assertFalse(cb.isAllowed());
    }

    @Test
    void testExposesDelegate() throws Exception {
        ResilienceCircuitBreaker cb = create();
        assertNotNull(cb.delegate());
    }

    @Test
    void testReconfigureChangesThresholdDynamically() throws Exception {
        ResilienceCircuitBreaker cb = create();
        // 运行时调到极敏感：threshold=1, window=5
        cb.reconfigure(1, 5, 200, 1);
        for (int i = 0; i < 5; i++) {
            cb.recordFailure(new RuntimeException("error-" + i));
        }
        // 5 次失败填满 window 5，失败率 100% >> 1% → 熔断打开
        assertEquals(SpiderCircuitBreaker.State.OPEN, cb.state());
        assertFalse(cb.isAllowed());
    }
}
