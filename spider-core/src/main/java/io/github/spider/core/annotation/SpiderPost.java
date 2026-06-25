package io.github.spider.core.annotation;

import java.lang.annotation.*;

/**
 * HTTP POST 请求。
 *
 * <pre>{@code
 * @SpiderPost("/users")
 * UserDTO createUser(@Body CreateUserRequest body);
 * }</pre>
 *
 * <p>默认情况下 POST 请求不进行重试——使用 {@link Retry} 来覆盖此行为。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SpiderPost {
    /** 请求路径。 */
    String value();
    /** 预留用于静态请求头（未来功能）。 */
    String[] headers() default {};
}
