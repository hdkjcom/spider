package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.annotation.SpiderClient;
import io.github.spider.core.annotation.SpiderGet;
import io.github.spider.core.client.SpiderClientFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SpiderAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpiderAutoConfiguration.class));

    @SpiderClient(name = "test-client", url = "http://localhost:8080")
    interface TestClient {
        @SpiderGet("/hello")
        String hello();
    }

    @Test
    void testSpiderClientFactoryCreated() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(SpiderClientFactory.class);
            assertThat(ctx).hasSingleBean(SpiderClientFactory.class);
        });
    }

    @Test
    void testDefaultBeansCreated() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(SpiderClientFactory.class);
            // Default transport, encoder, decoder should all be available
            assertThat(ctx.getBean("spiderTransport")).isNotNull();
            assertThat(ctx.getBean("spiderEncoder")).isNotNull();
            assertThat(ctx.getBean("spiderDecoder")).isNotNull();
        });
    }

    @Test
    void testPropertiesConfiguration() {
        contextRunner
                .withPropertyValues(
                        "spider.default-timeout=3000",
                        "spider.default-retry.max-attempts=5",
                        "spider.transport.connect-timeout=5000",
                        "spider.transport.read-timeout=15000")
                .run(ctx -> {
                    SpiderProperties props = ctx.getBean(SpiderProperties.class);
                    assertThat(props.getDefaultTimeout()).isEqualTo(3000);
                    assertThat(props.getDefaultRetry().getMaxAttempts()).isEqualTo(5);
                    assertThat(props.getTransport().getConnectTimeout()).isEqualTo(5000);
                    assertThat(props.getTransport().getReadTimeout()).isEqualTo(15000);
                });
    }

    @Test
    void testSpiderDisabled() {
        contextRunner
                .withPropertyValues("spider.enabled=false")
                .run(ctx -> {
                    // When disabled, SpiderClientFactory should not be created
                    assertThat(ctx.containsBean("spiderClientFactory")).isFalse();
                });
    }
}
