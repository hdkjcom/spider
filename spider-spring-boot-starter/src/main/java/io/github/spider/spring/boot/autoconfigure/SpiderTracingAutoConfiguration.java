package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.telemetry.TracingInterceptor;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spider 链路追踪自动配置类，在 classpath 中包含 OpenTelemetry 时自动激活，
 * 注册 TracingInterceptor 以在请求链路中传播 trace 上下文。
 */
@Configuration
@ConditionalOnClass({OpenTelemetry.class, TracingInterceptor.class})
@AutoConfigureAfter(SpiderAutoConfiguration.class)
public class SpiderTracingAutoConfiguration {

    /**
     * 创建链路追踪拦截器。
     *
     * @return TracingInterceptor 实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "tracingInterceptor")
    public SpiderInterceptor tracingInterceptor() {
        return new TracingInterceptor();
    }
}
