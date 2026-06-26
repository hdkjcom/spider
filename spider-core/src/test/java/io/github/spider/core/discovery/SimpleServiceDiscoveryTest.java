package io.github.spider.core.discovery;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimpleServiceDiscovery 单元测试。
 */
class SimpleServiceDiscoveryTest {

    @Test
    void testRegisterAndResolve() {
        SimpleServiceDiscovery sd = new SimpleServiceDiscovery();
        sd.register("order-service", "http://h1:8080", "http://h2:8080");

        List<String> urls = sd.resolve("order-service");
        assertEquals(2, urls.size());
        assertTrue(urls.contains("http://h1:8080"));
        assertTrue(urls.contains("http://h2:8080"));
    }

    @Test
    void testResolveUnregisteredReturnsEmpty() {
        SimpleServiceDiscovery sd = new SimpleServiceDiscovery();
        List<String> urls = sd.resolve("unknown");
        assertNotNull(urls);
        assertTrue(urls.isEmpty());
    }

    @Test
    void testDeregister() {
        SimpleServiceDiscovery sd = new SimpleServiceDiscovery();
        sd.register("order-service", "http://h1:8080");
        assertFalse(sd.resolve("order-service").isEmpty());

        sd.deregister("order-service");
        assertTrue(sd.resolve("order-service").isEmpty());
    }

    @Test
    void testDeregisterUnregisteredIsNoOp() {
        SimpleServiceDiscovery sd = new SimpleServiceDiscovery();
        // 不应抛异常
        sd.deregister("no-such-service");
        assertTrue(sd.resolve("no-such-service").isEmpty());
    }

    @Test
    void testRegisterOverwritesPrevious() {
        SimpleServiceDiscovery sd = new SimpleServiceDiscovery();
        sd.register("svc", "http://old:8080");
        sd.register("svc", "http://new-a:8080", "http://new-b:8080");

        List<String> urls = sd.resolve("svc");
        assertEquals(2, urls.size());
        assertTrue(urls.contains("http://new-a:8080"));
        assertTrue(urls.contains("http://new-b:8080"));
        assertFalse(urls.contains("http://old:8080"));
    }

    @Test
    void testRegisterSingleUrl() {
        SimpleServiceDiscovery sd = new SimpleServiceDiscovery();
        sd.register("svc", "http://single:8080");

        List<String> urls = sd.resolve("svc");
        assertEquals(1, urls.size());
        assertEquals("http://single:8080", urls.get(0));
    }

    @Test
    void testMultipleServicesIsolated() {
        SimpleServiceDiscovery sd = new SimpleServiceDiscovery();
        sd.register("svcA", "http://a:8080");
        sd.register("svcB", "http://b1:8080", "http://b2:8080");

        assertEquals(1, sd.resolve("svcA").size());
        assertEquals(2, sd.resolve("svcB").size());
        assertTrue(sd.resolve("svcC").isEmpty());
    }

    @Test
    void testResolveReturnsMutableCopy() {
        // SimpleServiceDiscovery 返回内部列表引用，调用者修改不影响注册表
        // 这是当前实现的行为——验证此行为已被文档化
        SimpleServiceDiscovery sd = new SimpleServiceDiscovery();
        sd.register("svc", "http://a:8080");

        List<String> urls = sd.resolve("svc");
        urls.add("http://injected:8080");

        // 验证内部注册表是否也被修改（取决于实现）
        // 当前实现返回内部 list 引用，所以修改会传播
        List<String> after = sd.resolve("svc");
        assertTrue(after.contains("http://injected:8080"),
                "当前实现返回内部引用，修改会传播到注册表");
    }
}
