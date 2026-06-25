package io.github.spider.core.annotation;

import java.lang.annotation.*;

/** Configures retry behavior for a method. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Retry {

    /** Backoff strategy. */
    enum BackoffStrategy {
        FIXED,
        EXPONENTIAL
    }

    /** Maximum number of attempts (including the initial call). */
    int maxAttempts() default 3;

    /** Base backoff delay between retries in milliseconds. */
    long backoffMillis() default 100;

    /** Backoff strategy (default FIXED). */
    BackoffStrategy backoffStrategy() default BackoffStrategy.FIXED;

    /** Maximum backoff in milliseconds (only for EXPONENTIAL). 0 = no cap. */
    long maxBackoffMillis() default 5000;

    /** Exception types that trigger a retry. Empty = all I/O exceptions. */
    Class<? extends Throwable>[] retryOn() default {};

    /** HTTP status codes that should NOT be retried (e.g. 404, 422). */
    int[] ignoreStatus() default {};
}
