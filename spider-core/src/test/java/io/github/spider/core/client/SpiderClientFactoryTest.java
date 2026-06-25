package io.github.spider.core.client;

import io.github.spider.core.annotation.SpiderClient;
import io.github.spider.core.annotation.SpiderGet;
import org.junit.jupiter.api.Test;

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

        assertThrows(SpiderClientException.class, () -> factory.create(NotAnnotatedClient.class));
    }

    @Test
    void testBuilderRequiresTransport() {
        assertThrows(SpiderClientException.class, () -> SpiderClientFactory.builder().build());
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
}
