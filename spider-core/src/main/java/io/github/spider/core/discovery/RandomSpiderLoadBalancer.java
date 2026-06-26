package io.github.spider.core.discovery;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Random load balancer for simple client-side distribution.
 */
public class RandomSpiderLoadBalancer implements SpiderLoadBalancer {

    @Override
    public String choose(String serviceName, List<String> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
    }
}
