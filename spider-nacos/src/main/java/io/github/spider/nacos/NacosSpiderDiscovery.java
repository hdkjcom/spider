package io.github.spider.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.github.spider.core.discovery.SpiderServiceDiscovery;
import io.github.spider.core.exception.SpiderServiceDiscoveryException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Properties;

/**
 * 基于 Nacos 的服务发现实现。
 * 使用 Nacos NamingService 将服务名解析为实例 URL 列表。
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

    /**
     * 通过 Nacos 服务器地址创建服务发现实例。
     *
     * @param serverAddr Nacos 服务器地址，例如 "localhost:8848"
     * @throws NacosException 如果连接 Nacos 服务器失败
     */
    public NacosSpiderDiscovery(String serverAddr) throws NacosException {
        Properties props = new Properties();
        props.setProperty("serverAddr", serverAddr);
        this.namingService = NacosFactory.createNamingService(props);
    }

    /**
     * 通过已有的 NamingService 实例创建服务发现实例。
     *
     * @param namingService Nacos 命名服务实例
     */
    public NacosSpiderDiscovery(NamingService namingService) {
        this.namingService = namingService;
    }

    /** 关闭与 Nacos 服务端的连接。 */
    public void close() throws NacosException {
        namingService.shutDown();
    }

    /**
     * 根据服务名解析出健康的服务实例 URL 列表。
     *
     * @param serviceName 服务名称
     * @return 可用的服务实例 URL 列表（仅包含健康且启用的实例）
     */
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
            throw new SpiderServiceDiscoveryException("Failed to resolve service: " + serviceName, e);
        }
    }
}
