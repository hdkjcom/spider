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

@Configuration
@ConditionalOnClass({MeterRegistry.class, MicrometerSpiderMetrics.class})
@AutoConfigureAfter(SpiderAutoConfiguration.class)
public class SpiderMetricsAutoConfiguration {

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public SpiderMetrics spiderMetrics(MeterRegistry registry) {
        return new MicrometerSpiderMetrics(registry);
    }
}
