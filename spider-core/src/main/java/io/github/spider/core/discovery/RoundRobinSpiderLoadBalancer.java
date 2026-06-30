package io.github.spider.core.discovery;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-robin load balancer keyed by service name.
 *
 * <p>The internal {@code counters} map grows without bound as new service names
 * are encountered. Historical entries are never evicted automatically.
 * In production environments with dynamic service discovery, consider
 * periodically rebuilding the {@code RoundRobinSpiderLoadBalancer} instance
 * or calling {@link #cleanup()} from an external scheduler.
 */
public class RoundRobinSpiderLoadBalancer implements SpiderLoadBalancer {

    private final ConcurrentMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    /**
     * Select an instance from the given list using round-robin via a
     * per-service-name {@link AtomicInteger} counter.
     *
     * <p><b>Note:</b> the internal {@code counters} map may grow over time
     * as historical service names accumulate. There is no built-in eviction.
     * Production deployments should periodically rebuild the load-balancer
     * instance or invoke {@link #cleanup()} from a maintenance thread.
     */
    @Override
    public String choose(String serviceName, List<String> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        AtomicInteger counter = counters.computeIfAbsent(serviceName, key -> new AtomicInteger());
        int index = Math.floorMod(counter.getAndIncrement(), instances.size());
        return instances.get(index);
    }

    /**
     * Placeholder for external cleanup of stale counter entries.
     *
     * <p>Current implementation is a no-op. Callers may extend this method
     * to remove entries whose service names no longer appear in the active
     * instance registry, e.g. {@code counters.keySet().removeIf(...)}.
     */
    public void cleanup() {
        // no-op: reserved for future eviction logic
    }
}
