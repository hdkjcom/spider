package io.github.spider.spring.boot.autoconfigure.discovery;

import io.github.spider.core.discovery.SpiderLoadBalancer;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 将 Spring Cloud 的 {@link LoadBalancerClient} 适配为 Spider 的 {@link SpiderLoadBalancer}。
 *
 * <p>这样 Spider 可以直接复用项目已有的 Spring Cloud LoadBalancer 体系（权重 / 健康检查 /
 * ZoneAware / 自定义 ReactiveLoadBalancer 等），与 {@link DiscoveryClientSpiderServiceDiscovery}
 * 配套：服务发现走 Spring Cloud，负载均衡策略也走 Spring Cloud，二者统一生效。
 *
 * <p>容错：当 {@link LoadBalancerClient} 为 {@code null}（Spring Cloud LoadBalancer 不在场）
 * 或 {@code choose(serviceName)} 选不到实例时，回退到对传入 {@code instances} 的本地轮询，
 * 保持与 {@code RoundRobinSpiderLoadBalancer} 一致的行为，不破坏 Spider 自有兜底链路。
 */
public class LoadBalancerSpiderLoadBalancer implements SpiderLoadBalancer {

    private final LoadBalancerClient loadBalancerClient;

    /** 回退轮询计数器，仅在 LoadBalancerClient 缺失或选不到时使用。 */
    private final ConcurrentMap<String, AtomicInteger> fallbackCounters = new ConcurrentHashMap<>();

    public LoadBalancerSpiderLoadBalancer(LoadBalancerClient loadBalancerClient) {
        this.loadBalancerClient = loadBalancerClient;
    }

    @Override
    public String choose(String serviceName, List<String> instances) {
        // 优先委托给 Spring Cloud LoadBalancer
        if (loadBalancerClient != null && serviceName != null) {
            try {
                ServiceInstance instance = loadBalancerClient.choose(serviceName);
                if (instance != null && instance.getUri() != null) {
                    return instance.getUri().toString();
                }
            } catch (Exception ignored) {
                // Spring Cloud LoadBalancer 异常时不影响 Spider 调用，落到回退策略
            }
        }
        return fallbackRoundRobin(serviceName, instances);
    }

    private String fallbackRoundRobin(String serviceName, List<String> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        AtomicInteger counter = fallbackCounters.computeIfAbsent(
                serviceName == null ? "" : serviceName, key -> new AtomicInteger());
        int index = Math.floorMod(counter.getAndIncrement(), instances.size());
        return instances.get(index);
    }
}
