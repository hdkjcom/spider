package io.github.spider.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.github.spider.core.discovery.SpiderServiceDiscovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Nacos-based service discovery implementation.
 * Uses Nacos NamingService to resolve service names to instance URLs.
 *
 * <pre>{@code
 * NacosSpiderDiscovery discovery = new NacosSpiderDiscovery("localhost:8848");
 * SpiderClientFactory.builder()
 *     .serviceDiscovery(discovery)
 *     .build();
 * }</pre>
 */
public class NacosSpiderDiscovery implements SpiderServiceDiscovery {

    private final NamingService namingService;

    public NacosSpiderDiscovery(String serverAddr) throws NacosException {
        Properties props = new Properties();
        props.setProperty("serverAddr", serverAddr);
        this.namingService = NacosFactory.createNamingService(props);
    }

    public NacosSpiderDiscovery(NamingService namingService) {
        this.namingService = namingService;
    }

    @Override
    public List<String> resolve(String serviceName) {
        try {
            List<Instance> instances = namingService.selectInstances(serviceName, true);
            List<String> urls = new ArrayList<>();
            for (Instance instance : instances) {
                if (instance.isHealthy() && instance.isEnabled()) {
                    urls.add("http://" + instance.getIp() + ":" + instance.getPort());
                }
            }
            return urls;
        } catch (NacosException e) {
            throw new RuntimeException("Failed to resolve service: " + serviceName, e);
        }
    }
}
