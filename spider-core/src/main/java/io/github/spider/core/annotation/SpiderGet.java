package io.github.spider.core.annotation;

import java.lang.annotation.*;

/**
 * HTTP GET request.
 * The path may contain placeholders ({@code {name}}) resolved by {@link Path} parameters.
 *
 * <pre>{@code
 * @SpiderGet("/users/{id}")
 * UserDTO getUser(@Path("id") Long id);
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SpiderGet {
    /** Request path with optional {@code {variable}} placeholders. */
    String value();
    /** Reserved for static headers (future). */
    String[] headers() default {};
}
