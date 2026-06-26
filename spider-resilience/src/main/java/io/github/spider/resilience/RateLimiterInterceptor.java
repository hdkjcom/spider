package io.github.spider.resilience;

import io.github.spider.core.annotation.RateLimit;
import io.github.spider.core.exception.SpiderRateLimitException;
import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import java.time.Duration;

/**
 * 基于 Resilience4j 的限流拦截器。
 * 当超过速率限制时快速失败，抛出异常。
 */
public class RateLimiterInterceptor implements SpiderInterceptor {

    private final RateLimiter rateLimiter;

    /**
     * 根据给定的名称和 {@link RateLimit} 注解配置创建限流拦截器。
     *
     * @param name       限流器名称，用于在注册中心中标识
     * @param annotation 限流注解，包含许可数、时间窗口、超时等配置
     */
    public RateLimiterInterceptor(String name, RateLimit annotation) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(annotation.permits())
                .limitRefreshPeriod(Duration.ofMillis(annotation.timeUnit().toMillis(annotation.duration())))
                .timeoutDuration(Duration.ofMillis(annotation.timeoutMillis()))
                .build();
        this.rateLimiter = RateLimiterRegistry.of(config).rateLimiter(name);
    }

    /**
     * 使用已有的 Resilience4j {@link RateLimiter} 实例创建限流拦截器。
     *
     * @param rateLimiter 已有的 Resilience4j RateLimiter 实例
     */
    public RateLimiterInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /**
     * 在请求发送前执行限流检查。
     * 如果超过速率限制，则抛出 {@link SpiderRateLimitException} 快速失败。
     *
     * @param request 即将发送的请求
     * @return 未经修改的原始请求（限流通过时）
     * @throws SpiderRateLimitException 当速率限制被触发时
     */
    @Override
    public SpiderRequest beforeRequest(SpiderRequest request) {
        if (!rateLimiter.acquirePermission()) {
            throw new SpiderRateLimitException(request.fullUrl());
        }
        return request;
    }

    /**
     * 在收到响应后执行处理，当前实现直接透传响应。
     *
     * @param response 从远程服务返回的响应
     * @return 未经修改的原始响应
     */
    @Override
    public SpiderResponse afterResponse(SpiderResponse response) {
        return response;
    }

    /**
     * 暴露底层 Resilience4j {@link RateLimiter} 实例，供高级用户直接使用。
     *
     * @return 底层 Resilience4j RateLimiter 实例
     */
    public RateLimiter delegate() {
        return rateLimiter;
    }
}
