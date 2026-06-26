package io.github.spider.core.client;

import io.github.spider.core.annotation.SpiderClient;
import io.github.spider.core.annotation.SpiderGet;
import io.github.spider.core.discovery.SimpleServiceDiscovery;
import io.github.spider.core.exception.SpiderConfigurationException;
import io.github.spider.core.exception.SpiderServiceDiscoveryException;
import io.github.spider.core.transport.SpiderResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SpiderClientFactoryTest {

    @SpiderClient(name = "test", url = "http://localhost:8080")
    interface TestClient {
        @SpiderGet("/hello")
        String hello();
    }

    interface NotAnnotatedClient {
        String hello();
    }

    @SpiderClient(name = "discovered-service")
    interface DiscoveredClient {
        @SpiderGet("/hello")
        String hello();
    }

    @SpiderClient(name = "dual-config", url = "http://annotation-url:8080")
    interface DualConfigClient {
        @SpiderGet("/hello")
        String hello();
    }

    @SpiderClient(name = "ws-service", url = "   ")
    interface WhitespaceUrlClient {
        @SpiderGet("/hello")
        String hello();
    }

    @Test
    void testCreateProxySuccess() {
        SpiderClientFactory factory = SpiderClientFactory.builder()
                .transport(request -> {
                    throw new UnsupportedOperationException("not implemented in test");
                })
                .build();

        TestClient client = factory.create(TestClient.class);
        assertNotNull(client);
    }

    @Test
    void testCreateProxyWithoutSpiderClientAnnotation() {
        SpiderClientFactory factory = SpiderClientFactory.builder()
                .transport(request -> {
                    throw new RuntimeException("no");
                })
                .build();

        assertThrows(SpiderConfigurationException.class, () -> factory.create(NotAnnotatedClient.class));
    }

    @Test
    void testBuilderRequiresTransport() {
        assertThrows(SpiderConfigurationException.class, () -> SpiderClientFactory.builder().build());
    }

    @Test
    void testProxyImplementsInterface() {
        SpiderClientFactory factory = SpiderClientFactory.builder()
                .transport(request -> {
                    throw new RuntimeException("no");
                })
                .build();

        TestClient client = factory.create(TestClient.class);
        assertTrue(client instanceof TestClient);
    }

    @Test
    void testServiceDiscoveryResolvesBaseUrl() {
        AtomicReference<String> fullUrl = new AtomicReference<>();
        SimpleServiceDiscovery discovery = new SimpleServiceDiscovery()
                .register("discovered-service", "http://localhost:9001");

        SpiderClientFactory factory = SpiderClientFactory.builder()
                .serviceDiscovery(discovery)
                .transport(request -> {
                    fullUrl.set(request.fullUrl());
                    return new SpiderResponse()
                            .statusCode(200)
                            .bodyBytes("ok".getBytes(StandardCharsets.UTF_8));
                })
                .decoder((bodyBytes, returnType) -> new String(bodyBytes, StandardCharsets.UTF_8))
                .build();

        DiscoveredClient client = factory.create(DiscoveredClient.class);

        assertEquals("ok", client.hello());
        assertEquals("http://localhost:9001/hello", fullUrl.get());
    }

    @Test
    void testServiceDiscoveryUsesRoundRobinByDefault() {
        AtomicReference<String> firstUrl = new AtomicReference<>();
        AtomicReference<String> secondUrl = new AtomicReference<>();
        SimpleServiceDiscovery discovery = new SimpleServiceDiscovery()
                .register("discovered-service", "http://localhost:9001", "http://localhost:9002");

        SpiderClientFactory factory = SpiderClientFactory.builder()
                .serviceDiscovery(discovery)
                .transport(request -> {
                    if (firstUrl.get() == null) {
                        firstUrl.set(request.fullUrl());
                    } else {
                        secondUrl.set(request.fullUrl());
                    }
                    return new SpiderResponse()
                            .statusCode(200)
                            .bodyBytes("ok".getBytes(StandardCharsets.UTF_8));
                })
                .decoder((bodyBytes, returnType) -> new String(bodyBytes, StandardCharsets.UTF_8))
                .build();

        DiscoveredClient client = factory.create(DiscoveredClient.class);
        client.hello();
        client.hello();

        assertEquals("http://localhost:9001/hello", firstUrl.get());
        assertEquals("http://localhost:9002/hello", secondUrl.get());
    }

    @Test
    void testServiceDiscoveryRequiresInstancesWhenUrlMissing() {
        SpiderClientFactory factory = SpiderClientFactory.builder()
                .transport(request -> new SpiderResponse().statusCode(200))
                .decoder((bodyBytes, returnType) -> null)
                .build();

        DiscoveredClient client = factory.create(DiscoveredClient.class);

        assertThrows(SpiderServiceDiscoveryException.class, client::hello);
    }

    @Test
    void testEmptyInstancesWhenDiscoveryConfigured() {
        SimpleServiceDiscovery discovery = new SimpleServiceDiscovery();
        // 未注册 discovered-service —— resolve 返回空列表

        SpiderClientFactory factory = SpiderClientFactory.builder()
                .transport(request -> new SpiderResponse().statusCode(200))
                .decoder((bodyBytes, returnType) -> null)
                .serviceDiscovery(discovery)
                .build();

        DiscoveredClient client = factory.create(DiscoveredClient.class);

        assertThrows(SpiderServiceDiscoveryException.class, client::hello);
    }

    @Test
    void testDeregisterBetweenCallsFailsSecondCall() {
        AtomicReference<String> firstUrl = new AtomicReference<>();
        SimpleServiceDiscovery discovery = new SimpleServiceDiscovery()
                .register("discovered-service", "http://localhost:9001");

        SpiderClientFactory factory = SpiderClientFactory.builder()
                .serviceDiscovery(discovery)
                .transport(request -> {
                    firstUrl.set(request.fullUrl());
                    return new SpiderResponse()
                            .statusCode(200)
                            .bodyBytes("ok".getBytes(StandardCharsets.UTF_8));
                })
                .decoder((bodyBytes, returnType) -> new String(bodyBytes, StandardCharsets.UTF_8))
                .build();

        DiscoveredClient client = factory.create(DiscoveredClient.class);

        // 第一次调用成功
        assertEquals("ok", client.hello());

        // 注销服务
        discovery.deregister("discovered-service");

        // 第二次调用应失败
        assertThrows(SpiderServiceDiscoveryException.class, client::hello);
    }

    @Test
    void testAnnotationUrlPrecedenceOverDiscovery() {
        AtomicReference<String> fullUrl = new AtomicReference<>();
        SimpleServiceDiscovery discovery = new SimpleServiceDiscovery()
                .register("dual-config", "http://discovery-url:9001");

        SpiderClientFactory factory = SpiderClientFactory.builder()
                .serviceDiscovery(discovery)
                .transport(request -> {
                    fullUrl.set(request.fullUrl());
                    return new SpiderResponse()
                            .statusCode(200)
                            .bodyBytes("ok".getBytes(StandardCharsets.UTF_8));
                })
                .decoder((bodyBytes, returnType) -> new String(bodyBytes, StandardCharsets.UTF_8))
                .build();

        DualConfigClient client = factory.create(DualConfigClient.class);

        assertEquals("ok", client.hello());
        // 应使用注解中的 URL 而非服务发现返回的 URL
        assertTrue(fullUrl.get().startsWith("http://annotation-url:8080"),
                "当注解指定 url 时，应优先使用注解 URL 而非服务发现: " + fullUrl.get());
    }

    @Test
    void testWhitespaceOnlyUrlFallsThroughToDiscovery() {
        AtomicReference<String> fullUrl = new AtomicReference<>();
        SimpleServiceDiscovery discovery = new SimpleServiceDiscovery()
                .register("ws-service", "http://discovery:9001");

        SpiderClientFactory factory = SpiderClientFactory.builder()
                .serviceDiscovery(discovery)
                .transport(request -> {
                    fullUrl.set(request.fullUrl());
                    return new SpiderResponse()
                            .statusCode(200)
                            .bodyBytes("ok".getBytes(StandardCharsets.UTF_8));
                })
                .decoder((bodyBytes, returnType) -> new String(bodyBytes, StandardCharsets.UTF_8))
                .build();

        WhitespaceUrlClient client = factory.create(WhitespaceUrlClient.class);

        assertEquals("ok", client.hello());
        // 空格 URL 被视为未设置，应回退到服务发现
        assertTrue(fullUrl.get().startsWith("http://discovery:9001"),
                "空格-only URL 应回退到服务发现: " + fullUrl.get());
    }
}
