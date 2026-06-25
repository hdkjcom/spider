package io.github.spider.core.annotation;

import java.lang.annotation.*;

/**
 * HTTP GET 请求。
 * 路径可包含占位符（{@code {name}}），由 {@link Path} 参数解析。
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
    /** 请求路径，可包含 {@code {variable}} 占位符。 */
    String value();
    /** 预留用于静态请求头（未来功能）。 */
    String[] headers() default {};
}
