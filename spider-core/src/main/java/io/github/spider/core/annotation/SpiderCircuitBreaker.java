package io.github.spider.core.annotation;

import java.lang.annotation.*;

/**
 * 为 @SpiderClient 接口配置熔断器。
 * 应用后，客户端代理将对所有调用进行熔断保护。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpiderCircuitBreaker {

    /** 失败率阈值，百分比（0-100）。超过该值时熔断器打开。 */
    int failureRateThreshold() default 50;

    /** 用于计算失败率的滑动窗口中的调用次数。 */
    int slidingWindowSize() default 10;

    /** 从 OPEN 状态转换到 HALF_OPEN 状态前的等待时间，单位毫秒。 */
    long waitDurationInOpenStateMillis() default 10000;

    /** HALF_OPEN 状态下允许的调用次数。 */
    int permittedNumberOfCallsInHalfOpenState() default 3;
}
