package io.github.spider.core.discovery;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RoundRobinSpiderLoadBalancer 单元测试。
 */
class RoundRobinSpiderLoadBalancerTest {

    private final RoundRobinSpiderLoadBalancer lb = new RoundRobinSpiderLoadBalancer();

    @Test
    void testSingleInstanceReturnsSame() {
        List<String> instances = Collections.singletonList("http://host:8080");
        String result = lb.choose("svc", instances);
        assertEquals("http://host:8080", result);
        // 多次调用返回同一实例
        assertEquals("http://host:8080", lb.choose("svc", instances));
        assertEquals("http://host:8080", lb.choose("svc", instances));
    }

    @Test
    void testMultiInstanceRoundRobin() {
        List<String> instances = Arrays.asList("http://h1:8080", "http://h2:8080", "http://h3:8080");
        assertEquals("http://h1:8080", lb.choose("svc", instances));
        assertEquals("http://h2:8080", lb.choose("svc", instances));
        assertEquals("http://h3:8080", lb.choose("svc", instances));
        // 回到第一个
        assertEquals("http://h1:8080", lb.choose("svc", instances));
    }

    @Test
    void testWrapAroundAfterFullCycle() {
        List<String> instances = Arrays.asList("A", "B");
        // 第一轮
        assertEquals("A", lb.choose("x", instances));
        assertEquals("B", lb.choose("x", instances));
        // 第二轮
        assertEquals("A", lb.choose("x", instances));
        assertEquals("B", lb.choose("x", instances));
        // 第三轮
        assertEquals("A", lb.choose("x", instances));
    }

    @Test
    void testPerServiceIsolation() {
        List<String> svcA = Arrays.asList("a1", "a2");
        List<String> svcB = Collections.singletonList("b1");

        assertEquals("a1", lb.choose("svcA", svcA));
        assertEquals("b1", lb.choose("svcB", svcB));
        assertEquals("a2", lb.choose("svcA", svcA));
        assertEquals("b1", lb.choose("svcB", svcB));
    }

    @Test
    void testNullInstancesReturnsNull() {
        assertNull(lb.choose("svc", null));
    }

    @Test
    void testEmptyInstancesReturnsNull() {
        assertNull(lb.choose("svc", Collections.emptyList()));
    }

    @Test
    void testDifferentServicesIndependentCounters() {
        List<String> instances = Arrays.asList("x1", "x2");
        // svc1 消耗第一个
        assertEquals("x1", lb.choose("svc1", instances));
        // svc2 从自己的第一个开始（独立计数）
        assertEquals("x1", lb.choose("svc2", instances));
        assertEquals("x2", lb.choose("svc2", instances));
        // svc1 继续从第二个
        assertEquals("x2", lb.choose("svc1", instances));
    }
}
