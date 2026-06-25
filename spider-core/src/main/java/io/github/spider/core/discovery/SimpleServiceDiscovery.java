package io.github.spider.core.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的简单服务发现实现。
 * 将服务名称映射到固定的 URL 列表，适用于测试和简单部署场景。
 */
public class SimpleServiceDiscovery implements SpiderServiceDiscovery {

    private final Map<String, List<String>> registry = new ConcurrentHashMap<>();

    /** 注册一个服务及其 URL 列表。 */
    public SimpleServiceDiscovery register(String serviceName, String... urls) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, urls);
        registry.put(serviceName, list);
        return this;
    }

    /** 移除一个服务注册。 */
    public SimpleServiceDiscovery deregister(String serviceName) {
        registry.remove(serviceName);
        return this;
    }

    @Override
    public List<String> resolve(String serviceName) {
        List<String> urls = registry.get(serviceName);
        return urls != null ? urls : Collections.emptyList();
    }
}
