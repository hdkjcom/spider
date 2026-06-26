package io.github.spider.core.discovery;

import java.util.List;

/**
 * Selects one service instance from resolved candidate URLs.
 */
public interface SpiderLoadBalancer {

    String choose(String serviceName, List<String> instances);
}
