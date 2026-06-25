package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.discovery.SpiderServiceDiscovery;
import io.github.spider.nacos.NacosSpiderDiscovery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(NacosSpiderDiscovery.class)
@ConditionalOnProperty(prefix = "spider.nacos", name = "server-addr")
@AutoConfigureAfter(SpiderAutoConfiguration.class)
public class SpiderNacosAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpiderServiceDiscovery spiderServiceDiscovery(
            @Value("${spider.nacos.server-addr}") String serverAddr) throws Exception {
        return new NacosSpiderDiscovery(serverAddr);
    }
}
