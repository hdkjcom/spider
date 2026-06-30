package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.discovery.SpiderServiceDiscovery;
import io.github.spider.spring.boot.autoconfigure.discovery.DiscoveryClientSpiderServiceDiscovery;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cloud 服务发现适配自动配置。
 *
 * <p>当 classpath 上存在 Spring Cloud 的 {@link DiscoveryClient} 且容器中已有其 Bean 时，
 * 自动提供一个 {@link SpiderServiceDiscovery} 实现，复用项目已有的 Spring Cloud 注册中心
 * （Nacos / Eureka / Consul / K8s），用户无需再配 {@code spider.nacos.*}。
 *
 * <p>优先级高于 {@link SpiderNacosAutoConfiguration}：若已通过 Spring Cloud 注册，
 * 原生 Nacos 配置将不生效；只有项目未接入 Spring Cloud 时，{@code spider.nacos.*}
 * 才作为兜底。
 */
@Configuration
@ConditionalOnClass(DiscoveryClient.class)
@ConditionalOnBean(DiscoveryClient.class)
@ConditionalOnMissingBean(SpiderServiceDiscovery.class)
@AutoConfigureBefore(SpiderNacosAutoConfiguration.class)
public class SpiderSpringCloudDiscoveryAutoConfiguration {

    @Bean
    public SpiderServiceDiscovery spiderServiceDiscovery(DiscoveryClient discoveryClient) {
        return new DiscoveryClientSpiderServiceDiscovery(discoveryClient);
    }
}
