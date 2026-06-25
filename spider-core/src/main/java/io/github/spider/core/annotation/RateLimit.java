package io.github.spider.core.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 对注解的方法或客户端进行限流。
 * 超过限制时请求立即失败。
 *
 * <p>在拦截器级别应用，位于传输调用之前。
 *
 * <pre>{@code
 * @RateLimit(permits = 100, duration = 1, timeUnit = TimeUnit.SECONDS)
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RateLimit {
    /** 每个时间窗口的最大许可数。 */
    int permits() default 100;
    /** 时间窗口长度。 */
    long duration() default 1;
    /** 时间窗口的单位（默认：秒）。 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    /** 获取许可的最大等待时间，单位毫秒。0 表示快速失败。 */
    long timeoutMillis() default 0;
}
