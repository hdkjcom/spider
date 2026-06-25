package io.github.spider.core.discovery;

import java.util.*;

/**
 * 服务发现抽象。将服务名解析为实例 URL 列表。
 *
 * <p>内置实现：{@code SimpleServiceDiscovery}（内存模式）、
 * {@code NacosSpiderDiscovery}（Nacos）。
 */
public interface SpiderServiceDiscovery {
    List<String> resolve(String serviceName);
    SpiderServiceDiscovery NOOP = name -> Collections.emptyList();
}
