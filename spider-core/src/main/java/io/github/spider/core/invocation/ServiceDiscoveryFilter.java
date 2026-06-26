package io.github.spider.core.invocation;

import io.github.spider.core.discovery.SpiderLoadBalancer;
import io.github.spider.core.discovery.SpiderServiceDiscovery;
import io.github.spider.core.exception.SpiderServiceDiscoveryException;

import java.util.List;

/**
 * 当 baseUrl 为空时，通过服务发现和负载均衡解析实际 URL。
 *
 * <p>如果 baseUrl 已设置，则直接透传。否则通过 serviceDiscovery 查找实例，
 * 再通过 loadBalancer 选择一个。
 */
public class ServiceDiscoveryFilter implements SpiderInvocationFilter {

    private final SpiderServiceDiscovery serviceDiscovery;
    private final SpiderLoadBalancer loadBalancer;

    public ServiceDiscoveryFilter(SpiderServiceDiscovery serviceDiscovery, SpiderLoadBalancer loadBalancer) {
        this.serviceDiscovery = serviceDiscovery;
        this.loadBalancer = loadBalancer;
    }

    @Override
    public Object filter(SpiderInvocationContext ctx, SpiderFilterChain chain) throws Throwable {
        String baseUrl = ctx.baseUrl();
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            ctx.setResolvedBaseUrl(baseUrl);
            return chain.next(ctx);
        }

        List<String> instances = serviceDiscovery.resolve(ctx.clientName());
        String selected = loadBalancer != null ? loadBalancer.choose(ctx.clientName(), instances) : null;
        if (selected == null || selected.trim().isEmpty()) {
            throw new SpiderServiceDiscoveryException("No service instance found for " + ctx.clientName());
        }
        ctx.setResolvedBaseUrl(selected);
        return chain.next(ctx);
    }
}
