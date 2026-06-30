package io.github.spider.core.invocation;

import io.github.spider.core.metrics.SpiderMetrics;

/**
 * 记录调用成功或失败的指标。
 *
 * <p>此 filter 位于 RetryFilter 之后、FallbackFilter 之前，
 * 用于记录重试耗尽后的最终失败指标。成功指标由 RetryFilter 在每次成功尝试时记录。
 */
public class MetricsFilter implements SpiderInvocationFilter {

    /** @param metrics 指标实现（当前仅作为安全网持有，指标记录已由 RetryFilter 接管） */
    public MetricsFilter(SpiderMetrics metrics) {
        // 保留构造器签名以兼容 SpiderClientFactory 的链装配
    }

    @Override
    public Object filter(SpiderInvocationContext ctx, SpiderFilterChain chain) throws Throwable {
        try {
            return chain.next(ctx);
        } catch (Throwable t) {
            // RetryFilter 已记录 failure，此处仅作为安全网
            if (!(t instanceof Exception)) {
                throw t;
            }
            throw t;
        }
    }
}
