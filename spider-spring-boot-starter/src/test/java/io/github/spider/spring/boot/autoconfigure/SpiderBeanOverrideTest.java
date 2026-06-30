package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.metrics.SpiderMetrics;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import io.github.spider.core.transport.SpiderTransport;
import io.github.spider.http.OkHttpSpiderTransport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证用户自定义 Bean 可以覆盖 Spider 自动配置提供的默认实现。
 *
 * <p>Spider 的默认 Bean（如 {@code spiderTransport}、{@code spiderMetrics}）均带有
 * {@code @ConditionalOnMissingBean}，因此当用户在自身 {@code @Configuration} 中声明同类型 Bean 时，
 * 自动配置的默认实现应被跳过，容器中实际生效的是用户提供的实例。</p>
 */
class SpiderBeanOverrideTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpiderAutoConfiguration.class));

    /** 用户自定义的 SpiderTransport 实现类，便于断言容器中实例正是用户提供的那个。 */
    static final class UserTransport implements SpiderTransport {
        @Override
        public SpiderResponse execute(SpiderRequest request) {
            // 测试桩实现，不会被实际调用
            return null;
        }
    }

    /** 用户自定义的 SpiderMetrics 标记类，与默认的 NOOP 区分。 */
    static final class UserMetrics implements SpiderMetrics {
    }

    @Configuration
    static class OverrideConfig {

        @Bean
        public SpiderTransport spiderTransport() {
            return new UserTransport();
        }

        @Bean
        public SpiderMetrics spiderMetrics() {
            return new UserMetrics();
        }
    }

    @Test
    void userTransportOverridesDefault() {
        contextRunner
                .withUserConfiguration(OverrideConfig.class)
                .run(ctx -> {
                    SpiderTransport transport = ctx.getBean(SpiderTransport.class);
                    assertThat(transport).isInstanceOf(UserTransport.class);
                    // 确认默认的 OkHttpSpiderTransport 未被使用
                    assertThat(transport).isNotInstanceOf(OkHttpSpiderTransport.class);
                });
    }

    @Test
    void userMetricsOverridesDefault() {
        contextRunner
                .withUserConfiguration(OverrideConfig.class)
                .run(ctx -> {
                    SpiderMetrics metrics = ctx.getBean(SpiderMetrics.class);
                    assertThat(metrics).isInstanceOf(UserMetrics.class);
                    // 默认实现是 SpiderMetrics.NOOP（匿名类），不应出现在容器中
                    assertThat(metrics).isNotSameAs(SpiderMetrics.NOOP);
                });
    }
}
