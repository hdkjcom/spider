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

/**
 * Spider Nacos 服务发现自动配置类，在 classpath 中包含 NacosSpiderDiscovery
 * 且配置了 {@code spider.nacos.server-addr} 时激活。
 *
 * <p>若项目已接入 Spring Cloud（存在 {@link io.github.spider.spring.boot.autoconfigure.discovery.DiscoveryClientSpiderServiceDiscovery}），
 * 则本配置不生效，避免重复创建 SpiderServiceDiscovery。
 */
@Configuration
@ConditionalOnClass(NacosSpiderDiscovery.class)
@ConditionalOnProperty(prefix = "spider.nacos", name = "server-addr")
@ConditionalOnMissingBean(SpiderServiceDiscovery.class)
@AutoConfigureAfter(SpiderAutoConfiguration.class)
public class SpiderNacosAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SpiderServiceDiscovery.class)
    public SpiderServiceDiscovery spiderServiceDiscovery(
            @Value("${spider.nacos.server-addr}") String serverAddr) throws Exception {
        return new NacosSpiderDiscovery(serverAddr);
    }
}
