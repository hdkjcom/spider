package io.github.spider.core.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory service discovery implementation.
 * Maps service names to fixed URLs. Useful for testing and simple deployments.
 */
public class SimpleServiceDiscovery implements SpiderServiceDiscovery {

    private final Map<String, List<String>> registry = new ConcurrentHashMap<>();

    /** Register a service with its URLs. */
    public SimpleServiceDiscovery register(String serviceName, String... urls) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, urls);
        registry.put(serviceName, list);
        return this;
    }

    /** Remove a service registration. */
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
