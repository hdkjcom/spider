package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.annotation.RateLimit;
import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.resilience.RateLimiterInterceptor;
import io.github.spider.resilience.ResilienceCircuitBreaker;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

/**
 * Spider 弹性治理自动配置类，在 classpath 中包含 Resilience4j 时自动激活，
 * 注册限流拦截器。
 */
@Configuration
@ConditionalOnClass(ResilienceCircuitBreaker.class)
@AutoConfigureAfter(SpiderAutoConfiguration.class)
public class SpiderResilienceAutoConfiguration {

    /**
     * 创建基于 Resilience4j 的限流拦截器。
     *
     * @return RateLimiterInterceptor 实例
     */
    @Bean
    @ConditionalOnClass(RateLimiterInterceptor.class)
    @ConditionalOnMissingBean
    public SpiderInterceptor rateLimiterInterceptor() {
        return new RateLimiterInterceptor(
                io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("spider-default"));
    }
}
