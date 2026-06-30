package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.annotation.SpiderClient;
import io.github.spider.core.annotation.SpiderGet;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link EnableSpiderClients} 能够扫描并注册标注了 {@code @SpiderClient} 的接口为 Spring Bean。
 *
 * <p>使用 {@link ApplicationContextRunner} 加载一个带有 {@code @EnableSpiderClients} 的测试配置类，
 * 断言测试接口被注册为 Bean、可以通过名称或类型获取，且 Bean 类型为接口本身（由
 * {@link SpiderClientFactoryBean} 暴露）。</p>
 */
class SpiderClientScanTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpiderAutoConfiguration.class));

    /**
     * 被扫描的测试客户端接口。位于本测试类的包路径下，{@code basePackageClasses} 指向本类即可覆盖。
     */
    @SpiderClient(name = "test-scan", url = "http://localhost:9999")
    interface ScanClient {
        @SpiderGet("/ping")
        String ping();
    }

    /** 承载 {@code @EnableSpiderClients} 的配置类，扫描 ScanClient 所在包。 */
    @Configuration
    @EnableSpiderClients(basePackageClasses = ScanClient.class)
    static class ScanConfig {
    }

    @Test
    void scannedClientIsRegisteredAsBean() {
        contextRunner
                .withUserConfiguration(ScanConfig.class)
                .run(ctx -> {
                    // 通过约定 bean 名称（uncapitalize(name) + "SpiderClient"）获取
                    assertThat(ctx.containsBean("test-scanSpiderClient")).isTrue();
                    // 通过接口全限定名别名获取（支持按类型注入）
                    assertThat(ctx.containsBean(ScanClient.class.getName())).isTrue();
                });
    }

    @Test
    void scannedClientIsGettableByType() {
        contextRunner
                .withUserConfiguration(ScanConfig.class)
                .run(ctx -> {
                    // SpiderClientFactoryBean.getObjectType() 返回接口类型，
                    // 因此容器中的 bean 可按接口类型注入
                    assertThat(ctx).hasSingleBean(ScanClient.class);
                    ScanClient client = ctx.getBean(ScanClient.class);
                    assertThat(client).isNotNull();
                    // JDK 动态代理，实现 ScanClient 接口
                    assertThat(ScanClient.class.isInstance(client)).isTrue();
                });
    }

    @Test
    void noScanNoRegistration() {
        // 不引入 @EnableSpiderClients 配置类时，不应有任何 ScanClient bean
        contextRunner.run(ctx -> {
            assertThat(ctx.getBeanNamesForType(ScanClient.class)).isEmpty();
        });
    }
}
