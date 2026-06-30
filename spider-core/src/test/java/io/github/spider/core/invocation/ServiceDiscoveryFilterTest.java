package io.github.spider.core.invocation;

import io.github.spider.core.discovery.SpiderLoadBalancer;
import io.github.spider.core.discovery.SpiderServiceDiscovery;
import io.github.spider.core.exception.SpiderServiceDiscoveryException;
import io.github.spider.core.metadata.MethodMetadata;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link ServiceDiscoveryFilter}：
 * baseUrl 非空时透传，为空时通过 discovery + loadBalancer 解析，
 * 无实例时抛 {@link SpiderServiceDiscoveryException}。
 *
 * <p>SPI 用匿名内部类实现，链尾用 sentinel filter 断言透传。
 */
class ServiceDiscoveryFilterTest {

    /** 链尾哨兵：返回固定标记，证明透传路径被走完。 */
    private static SpiderInvocationFilter sentinel() {
        return (ctx, chain) -> "passed-through";
    }

    @Test
    void nonEmptyBaseUrlPassesThroughWithoutDiscovery() throws Throwable {
        final AtomicInteger discoveryCalls = new AtomicInteger();
        SpiderServiceDiscovery discovery = name -> {
            discoveryCalls.incrementAndGet();
            return Collections.singletonList("http://should-not-be-used:9000");
        };
        SpiderLoadBalancer loadBalancer = (name, instances) -> "http://should-not-be-used:9000";

        ServiceDiscoveryFilter filter = new ServiceDiscoveryFilter(discovery, loadBalancer);
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "my-service", null, null, new MethodMetadata(), "http://preset:8080");
        SpiderFilterChain chain = new SpiderFilterChain(Arrays.asList(filter, sentinel()));

        Object result = filter.filter(ctx, new SpiderFilterChain(Arrays.asList(filter, sentinel())));

        assertEquals("passed-through", result);
        assertEquals("http://preset:8080", ctx.resolvedBaseUrl(),
                "baseUrl 非空时 resolvedBaseUrl 必须等于入参 baseUrl");
        assertEquals(0, discoveryCalls.get(), "baseUrl 非空时不得调用 serviceDiscovery");
    }

    @Test
    void emptyBaseUrlResolvesViaDiscoveryAndLoadBalancer() throws Throwable {
        final List<String> resolveArgs = new ArrayList<>();
        SpiderServiceDiscovery discovery = name -> {
            resolveArgs.add(name);
            return Arrays.asList("http://inst1:9000", "http://inst2:9000");
        };
        final List<List<String>> lbSeenInstances = new ArrayList<>();
        SpiderLoadBalancer loadBalancer = (name, instances) -> {
            lbSeenInstances.add(new ArrayList<>(instances));
            return "http://inst1:9000";
        };

        ServiceDiscoveryFilter filter = new ServiceDiscoveryFilter(discovery, loadBalancer);
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "order-service", null, null, new MethodMetadata(), null);

        Object result = filter.filter(ctx, new SpiderFilterChain(Collections.singletonList(sentinel())));

        assertEquals("passed-through", result);
        assertEquals("http://inst1:9000", ctx.resolvedBaseUrl());
        assertEquals(Collections.singletonList("order-service"), resolveArgs,
                "discovery 必须以 clientName 为参数调用");
        assertEquals(1, lbSeenInstances.size());
        assertEquals(Arrays.asList("http://inst1:9000", "http://inst2:9000"), lbSeenInstances.get(0),
                "loadBalancer 必须收到 discovery 返回的实例列表");
    }

    @Test
    void emptyInstancesThrowsDiscoveryException() {
        SpiderServiceDiscovery discovery = name -> Collections.emptyList();
        SpiderLoadBalancer loadBalancer = (name, instances) -> null;

        ServiceDiscoveryFilter filter = new ServiceDiscoveryFilter(discovery, loadBalancer);
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "ghost-service", null, null, new MethodMetadata(), null);

        SpiderServiceDiscoveryException ex = assertThrows(
                SpiderServiceDiscoveryException.class,
                () -> filter.filter(ctx, new SpiderFilterChain(Collections.singletonList(sentinel())))
        );
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("ghost-service"),
                "异常消息应包含服务名: " + ex.getMessage());
    }
}
