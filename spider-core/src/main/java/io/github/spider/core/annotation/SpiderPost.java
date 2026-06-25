package io.github.spider.core.annotation;

import java.lang.annotation.*;

/**
 * HTTP POST request.
 *
 * <pre>{@code
 * @SpiderPost("/users")
 * UserDTO createUser(@Body CreateUserRequest body);
 * }</pre>
 *
 * <p>By default POST requests are not retried — use {@link Retry} to override.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SpiderPost {
    /** Request path. */
    String value();
    /** Reserved for static headers (future). */
    String[] headers() default {};
}
