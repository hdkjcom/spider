package io.github.spider.core.client;

import io.github.spider.core.policy.SpiderCircuitBreaker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CountingCircuitBreakerTest {

    @io.github.spider.core.annotation.SpiderCircuitBreaker(
        failureRateThreshold = 50,
        slidingWindowSize = 10,
        waitDurationInOpenStateMillis = 100,
        permittedNumberOfCallsInHalfOpenState = 3
    )
    interface TestConfig {}

    private CountingCircuitBreaker create() throws Exception {
        io.github.spider.core.annotation.SpiderCircuitBreaker ann =
                TestConfig.class.getAnnotation(io.github.spider.core.annotation.SpiderCircuitBreaker.class);
        return new CountingCircuitBreaker(ann);
    }

    @Test
    void testInitiallyClosed() throws Exception {
        CountingCircuitBreaker cb = create();
        assertEquals(SpiderCircuitBreaker.State.CLOSED, cb.state());
        assertTrue(cb.isAllowed());
    }

    @Test
    void testOpensWhenFailureThresholdExceeded() throws Exception {
        CountingCircuitBreaker cb = create();
        // Simulate 5 failures out of 10 → 50% failure rate → threshold met
        for (int i = 0; i < 5; i++) {
            cb.recordFailure(new RuntimeException("error"));
        }
        for (int i = 0; i < 5; i++) {
            cb.recordSuccess();
        }
        // Window resets after slidingWindowSize reached, but let's force more failures
        for (int i = 0; i < 10; i++) {
            cb.recordFailure(new RuntimeException("error"));
        }
        assertEquals(SpiderCircuitBreaker.State.OPEN, cb.state());
        assertFalse(cb.isAllowed());
    }

    @Test
    void testTransitionsToHalfOpenAfterWaitDuration() throws Exception {
        CountingCircuitBreaker cb = create();
        // Force open
        for (int i = 0; i < 10; i++) {
            cb.recordFailure(new RuntimeException("error"));
        }
        assertEquals(SpiderCircuitBreaker.State.OPEN, cb.state());

        // Wait for the cooldown (100ms)
        Thread.sleep(150);

        // Now should be allowed (transitions to HALF_OPEN)
        assertTrue(cb.isAllowed());
        assertEquals(SpiderCircuitBreaker.State.HALF_OPEN, cb.state());
    }

    @Test
    void testLimitedCallsInHalfOpen() throws Exception {
        CountingCircuitBreaker cb = create();
        // Force open then wait
        for (int i = 0; i < 10; i++) {
            cb.recordFailure(new RuntimeException("error"));
        }
        Thread.sleep(150);

        // First 3 calls allowed (permittedNumberOfCallsInHalfOpenState = 3)
        assertTrue(cb.isAllowed());
        assertTrue(cb.isAllowed());
        assertTrue(cb.isAllowed());
        // 4th call NOT allowed
        assertFalse(cb.isAllowed());
    }

    @Test
    void testSuccessInHalfOpenClosesCircuit() throws Exception {
        CountingCircuitBreaker cb = create();
        // Force open then wait
        for (int i = 0; i < 10; i++) {
            cb.recordFailure(new RuntimeException("error"));
        }
        Thread.sleep(150);

        // Enter HALF_OPEN
        cb.isAllowed();
        // Record enough successes
        cb.recordSuccess();
        cb.recordSuccess();
        cb.recordSuccess();

        assertEquals(SpiderCircuitBreaker.State.CLOSED, cb.state());
    }
}
