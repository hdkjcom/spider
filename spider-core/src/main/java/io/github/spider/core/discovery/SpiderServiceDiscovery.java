package io.github.spider.core.discovery;

import java.util.*;

/**
 * Service discovery abstraction. Resolves service names to instance URLs.
 *
 * <p>Provided implementations: {@code SimpleServiceDiscovery} (in-memory),
 * {@code NacosSpiderDiscovery} (Nacos).
 */
public interface SpiderServiceDiscovery {
    List<String> resolve(String serviceName);
    SpiderServiceDiscovery NOOP = name -> Collections.emptyList();
}
