package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.metrics.SpiderMetrics;
import io.github.spider.metrics.MicrometerSpiderMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spider 指标自动配置类，在 classpath 中包含 Micrometer 时自动激活，
 * 使用 MicrometerSpiderMetrics 替换默认的 NOOP 指标实现。
 */
@Configuration
@ConditionalOnClass({MeterRegistry.class, MicrometerSpiderMetrics.class})
@AutoConfigureAfter(SpiderAutoConfiguration.class)
public class SpiderMetricsAutoConfiguration {

    /**
     * 创建基于 Micrometer 的 Spider 指标实现。
     *
     * @param registry Micrometer MeterRegistry
     * @return MicrometerSpiderMetrics 实例
     */
    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public SpiderMetrics spiderMetrics(MeterRegistry registry) {
        return new MicrometerSpiderMetrics(registry);
    }
}
