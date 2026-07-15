package io.github.spider.core.annotation;

import java.lang.annotation.*;

/** Marks a method parameter as the HTTP request body. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Body {

    /**
     * 请求体 Content-Type，覆盖 encoder 默认值。
     *
     * <p>空字符串（默认）表示使用 encoder 声明的媒体类型（通常为 {@code application/json}）。
     * 用于发送非 JSON 请求体，例如 {@code @Body(contentType = "application/xml") byte[] xml}。
     *
     * @return Content-Type 字符串，空字符串表示用 encoder 默认
     */
    String contentType() default "";
}
