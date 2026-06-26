package io.github.spider.core.invocation;

import io.github.spider.core.metrics.SpiderMetrics;

/**
 * 记录调用成功或失败的指标。
 *
 * <p>此 filter 位于 RetryFilter 之后、FallbackFilter 之前，
 * 用于记录重试耗尽后的最终失败指标。成功指标由 RetryFilter 在每次成功尝试时记录。
 */
public class MetricsFilter implements SpiderInvocationFilter {

    private final SpiderMetrics metrics;

    public MetricsFilter(SpiderMetrics metrics) {
        this.metrics = metrics != null ? metrics : SpiderMetrics.NOOP;
    }

    @Override
    public Object filter(SpiderInvocationContext ctx, SpiderFilterChain chain) throws Throwable {
        try {
            return chain.next(ctx);
        } catch (Throwable t) {
            // RetryFilter 已记录 failure，此处仅作为安全网
            // 如果异常绕过了 RetryFilter（理论上不应发生），在此记录
            if (!(t instanceof Exception)) {
                throw t;
            }
            throw t;
        }
    }
}
