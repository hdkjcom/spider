package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.config.SpiderConfigCenter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 动态配置自动配置。当容器中存在 {@link SpiderConfigCenter} Bean 时，
 * 自动创建 {@link ConfigOverrideFilter} 并绑定到 SpiderClientFactory 的过滤器链中。
 *
 * <p>支持的动态配置键（在配置中心实时变更即生效）：
 * <ul>
 *   <li>{@code spider.client.<name>.retry.backoff} — 重试退避间隔（毫秒）</li>
 *   <li>{@code spider.client.<name>.timeout} — 调用超时（毫秒）</li>
 * </ul>
 *
 * <p>未配置的 key 不影响现有行为（回退到注解/Spring 属性/Builder 默认值）。
 */
@Configuration
@ConditionalOnClass(SpiderConfigCenter.class)
@ConditionalOnBean(SpiderConfigCenter.class)
@AutoConfigureAfter(SpiderAutoConfiguration.class)
public class SpiderConfigAutoConfiguration {

    @Bean
    public ConfigOverrideFilter configOverrideFilter(SpiderConfigCenter configCenter) {
        return new ConfigOverrideFilter(configCenter);
    }
}
