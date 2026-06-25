package io.github.spider.core.annotation;

import java.lang.annotation.*;

/**
 * Configures circuit breaker for a @SpiderClient interface.
 * When applied, the client proxy will wrap all calls with circuit breaker protection.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpiderCircuitBreaker {

    /** Failure rate threshold in percentage (0-100). When exceeded, the circuit opens. */
    int failureRateThreshold() default 50;

    /** Number of calls in the sliding window for calculating failure rate. */
    int slidingWindowSize() default 10;

    /** Wait time in milliseconds before transitioning from OPEN to HALF_OPEN. */
    long waitDurationInOpenStateMillis() default 10000;

    /** Number of permitted calls in HALF_OPEN state. */
    int permittedNumberOfCallsInHalfOpenState() default 3;
}
