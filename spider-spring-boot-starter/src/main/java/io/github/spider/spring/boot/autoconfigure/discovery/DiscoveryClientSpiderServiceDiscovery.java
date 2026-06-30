package io.github.spider.spring.boot.autoconfigure.discovery;

import io.github.spider.core.discovery.SpiderServiceDiscovery;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 将 Spring Cloud 的 {@link DiscoveryClient} 适配为 Spider 的 {@link SpiderServiceDiscovery}。
 *
 * <p>这样 Spider 可以直接复用项目已有的 Spring Cloud 服务发现体系
 * （Nacos / Eureka / Consul / K8s 等），无需用户再为 Spider 单独配置
 * {@code spider.nacos.server-addr}。只要项目配了
 * {@code spring.cloud.nacos.discovery.*}（或其他注册中心），
 * Spider 接口不写 {@code url} 时即可自动发现实例。
 */
public class DiscoveryClientSpiderServiceDiscovery implements SpiderServiceDiscovery {

    private final DiscoveryClient discoveryClient;

    public DiscoveryClientSpiderServiceDiscovery(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public List<String> resolve(String serviceName) {
        if (serviceName == null) {
            return new ArrayList<>();
        }
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances == null || instances.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> urls = new ArrayList<>(instances.size());
        for (ServiceInstance instance : instances) {
            if (instance.getUri() != null) {
                urls.add(instance.getUri().toString());
            }
        }
        return urls;
    }
}
