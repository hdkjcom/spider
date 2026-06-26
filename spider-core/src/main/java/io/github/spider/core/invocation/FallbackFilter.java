package io.github.spider.core.invocation;

import io.github.spider.core.exception.SpiderFallbackException;
import io.github.spider.core.metrics.SpiderMetrics;
import io.github.spider.core.policy.FallbackFactory;
import io.github.spider.core.runtime.SpiderRuntime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 在所有重试耗尽后执行降级逻辑。
 *
 * <p>如果原始接口定义了 fallback 或 fallbackFactory，则调用它们并返回降级结果。
 * 否则重新抛出原始异常。
 */
public class FallbackFilter implements SpiderInvocationFilter {

    private static final Logger log = LoggerFactory.getLogger(FallbackFilter.class);

    private final Map<Method, Object> fallbackMap;
    private final SpiderMetrics metrics;

    public FallbackFilter(Map<Method, Object> fallbackMap, SpiderMetrics metrics) {
        this.fallbackMap = fallbackMap;
        this.metrics = metrics != null ? metrics : SpiderMetrics.NOOP;
    }

    @Override
    public Object filter(SpiderInvocationContext ctx, SpiderFilterChain chain) throws Throwable {
        try {
            return chain.next(ctx);
        } catch (Throwable t) {
            Exception cause = t instanceof Exception ? (Exception) t : new RuntimeException(t);

            Object fallbackOrFactory = fallbackMap.get(ctx.method());
            if (fallbackOrFactory != null) {
                try {
                    metrics.recordFallback(ctx.clientName(), ctx.method().getName());
                    SpiderRuntime.getInstance().recordFallback(ctx.clientName());
                    log.info("{} {} 触发降级", ctx.clientName(), ctx.request() != null
                            ? ctx.request().fullUrl() : ctx.method().getName());
                    Object fallbackInstance = resolveFallback(fallbackOrFactory, cause);
                    return ctx.method().invoke(fallbackInstance, ctx.args());
                } catch (Exception fbEx) {
                    throw new SpiderFallbackException("Fallback failed for " + ctx.method().getName(), fbEx);
                }
            }

            throw t;
        }
    }

    @SuppressWarnings("unchecked")
    private Object resolveFallback(Object fallbackOrFactory, Throwable cause) throws Exception {
        if (fallbackOrFactory instanceof FallbackFactory) {
            return ((FallbackFactory<Object>) fallbackOrFactory).create(cause);
        }
        return fallbackOrFactory;
    }
}
