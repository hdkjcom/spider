package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.annotation.RateLimit;
import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.resilience.RateLimiterInterceptor;
import io.github.spider.resilience.ResilienceCircuitBreaker;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
@ConditionalOnClass(ResilienceCircuitBreaker.class)
@AutoConfigureAfter(SpiderAutoConfiguration.class)
public class SpiderResilienceAutoConfiguration {

    @Bean
    @ConditionalOnClass(RateLimiterInterceptor.class)
    public SpiderInterceptor rateLimiterInterceptor() {
        return new RateLimiterInterceptor(
                io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("spider-default"));
    }
}
