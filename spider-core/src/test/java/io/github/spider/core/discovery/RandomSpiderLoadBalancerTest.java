package io.github.spider.core.discovery;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RandomSpiderLoadBalancer 单元测试。
 */
class RandomSpiderLoadBalancerTest {

    private final RandomSpiderLoadBalancer lb = new RandomSpiderLoadBalancer();

    @Test
    void testSingleInstanceReturnsSame() {
        List<String> instances = Collections.singletonList("http://host:8080");
        for (int i = 0; i < 10; i++) {
            assertEquals("http://host:8080", lb.choose("svc", instances));
        }
    }

    @Test
    void testPicksValidInstance() {
        List<String> instances = Arrays.asList("a", "b", "c");
        for (int i = 0; i < 100; i++) {
            String result = lb.choose("svc", instances);
            assertTrue(instances.contains(result));
        }
    }

    @Test
    void testDistributionCoversAllInstances() {
        List<String> instances = Arrays.asList("a", "b", "c");
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            seen.add(lb.choose("svc", instances));
        }
        assertEquals(3, seen.size(), "100次随机选择应覆盖全部3个实例");
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
    void testServiceNameParameterIgnoredForSelection() {
        // Random 负载均衡不关心服务名（无状态）
        List<String> instances = Arrays.asList("x", "y");
        String r1 = lb.choose("svcA", instances);
        String r2 = lb.choose("svcB", instances);
        // 两者都应来自同一列表
        assertTrue(instances.contains(r1));
        assertTrue(instances.contains(r2));
    }
}
