package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.discovery.SpiderLoadBalancer;
import io.github.spider.spring.boot.autoconfigure.discovery.LoadBalancerSpiderLoadBalancer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cloud LoadBalancer 适配自动配置。
 *
 * <p>当 classpath 上存在 Spring Cloud 的 {@link LoadBalancerClient} 且容器中已有其 Bean 时，
 * 自动提供 {@link LoadBalancerSpiderLoadBalancer} 作为 {@link SpiderLoadBalancer} 实现，
 * 复用项目已配置的 Spring Cloud 负载均衡策略（权重 / 健康检查 / ZoneAware 等），
 * 与 {@link SpiderSpringCloudDiscoveryAutoConfiguration} 配套，服务发现与负载均衡统一走 Spring Cloud。
 *
 * <p>容错：Spring Cloud LoadBalancer 不在场时本配置不生效，
 * Spider 仍使用自有的 {@code RoundRobinSpiderLoadBalancer}，行为不受影响。
 */
@Configuration
@ConditionalOnClass(LoadBalancerClient.class)
@ConditionalOnBean(LoadBalancerClient.class)
@ConditionalOnMissingBean(SpiderLoadBalancer.class)
public class SpiderLoadBalancerAutoConfiguration {

    @Bean
    public SpiderLoadBalancer spiderLoadBalancer(LoadBalancerClient loadBalancerClient) {
        return new LoadBalancerSpiderLoadBalancer(loadBalancerClient);
    }
}
