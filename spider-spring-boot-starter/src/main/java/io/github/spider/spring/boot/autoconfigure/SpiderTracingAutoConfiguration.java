package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.telemetry.TracingInterceptor;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({OpenTelemetry.class, TracingInterceptor.class})
@AutoConfigureAfter(SpiderAutoConfiguration.class)
public class SpiderTracingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "tracingInterceptor")
    public SpiderInterceptor tracingInterceptor() {
        return new TracingInterceptor();
    }
}
