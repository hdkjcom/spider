package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.config.SpiderConfigCenter;
import io.github.spider.core.invocation.SpiderFilterChain;
import io.github.spider.core.invocation.SpiderInvocationContext;
import io.github.spider.core.invocation.SpiderInvocationFilter;

/**
 * 动态配置覆盖过滤器。每次调用前从 {@link SpiderConfigCenter} 读取配置覆盖值，
 * 注入到 {@link SpiderInvocationContext#attributes()} 中，供下游 filter 使用。
 *
 * <p>覆盖的配置键（按 clientName 区分）：
 * <ul>
 *   <li>{@code spider.client.<name>.retry.backoff} — 退避间隔（毫秒）</li>
 *   <li>{@code spider.client.<name>.timeout} — 超时时间（毫秒）</li>
 * </ul>
 *
 * <p>当 ConfigCenter 不在场时，SpiderConfigFilterAutoConfiguration 不会创建本 filter，
 * 调用链路与现有行为完全一致，零影响。
 */
public class ConfigOverrideFilter implements SpiderInvocationFilter {

    private final SpiderConfigCenter configCenter;

    public ConfigOverrideFilter(SpiderConfigCenter configCenter) {
        this.configCenter = configCenter != null ? configCenter : SpiderConfigCenter.NOOP;
    }

    @Override
    public Object filter(SpiderInvocationContext ctx, SpiderFilterChain chain) throws Throwable {
        String prefix = "spider.client." + ctx.clientName() + ".";
        String backoff = configCenter.get(prefix + "retry.backoff", null);
        if (backoff != null) {
            try { ctx.setAttribute("config.backoffMillis", Long.parseLong(backoff)); }
            catch (NumberFormatException ignored) {}
        }
        String timeout = configCenter.get(prefix + "timeout", null);
        if (timeout != null) {
            try { ctx.setAttribute("config.timeout", Integer.parseInt(timeout)); }
            catch (NumberFormatException ignored) {}
        }
        return chain.next(ctx);
    }
}
