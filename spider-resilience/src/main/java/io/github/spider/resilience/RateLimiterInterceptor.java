package io.github.spider.resilience;

import io.github.spider.core.annotation.RateLimit;
import io.github.spider.core.client.SpiderClientException;
import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import java.time.Duration;

/**
 * Resilience4j-based rate limiting interceptor.
 * Fails fast when the rate limit is exceeded.
 */
public class RateLimiterInterceptor implements SpiderInterceptor {

    private final RateLimiter rateLimiter;

    public RateLimiterInterceptor(String name, RateLimit annotation) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(annotation.permits())
                .limitRefreshPeriod(Duration.ofMillis(annotation.timeUnit().toMillis(annotation.duration())))
                .timeoutDuration(Duration.ofMillis(annotation.timeoutMillis()))
                .build();
        this.rateLimiter = RateLimiterRegistry.of(config).rateLimiter(name);
    }

    public RateLimiterInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public SpiderRequest beforeRequest(SpiderRequest request) {
        if (!rateLimiter.acquirePermission()) {
            throw new SpiderClientException("Rate limit exceeded for " + request.fullUrl());
        }
        return request;
    }

    @Override
    public SpiderResponse afterResponse(SpiderResponse response) {
        return response;
    }

    public RateLimiter delegate() { return rateLimiter; }
}
