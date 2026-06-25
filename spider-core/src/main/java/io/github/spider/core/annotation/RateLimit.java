package io.github.spider.core.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Rate limits calls to the annotated method or client.
 * When the limit is exceeded the request fails immediately.
 *
 * <p>Applied at the interceptor level, before the transport call.
 *
 * <pre>{@code
 * @RateLimit(permits = 100, duration = 1, timeUnit = TimeUnit.SECONDS)
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RateLimit {
    /** Maximum permits per time window. */
    int permits() default 100;
    /** Time window length. */
    long duration() default 1;
    /** Time unit for the window (default: seconds). */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    /** Maximum wait time for a permit in milliseconds. 0 means fail-fast. */
    long timeoutMillis() default 0;
}
