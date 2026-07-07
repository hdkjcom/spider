package io.github.spider.core.invocation;

import io.github.spider.core.client.SpiderResponseContext;

/**
 * 管理 {@link SpiderResponseContext} ThreadLocal 的生命周期。
 *
 * <p>在调用链之前清理旧状态，成功时设置当前响应以便用户代码通过
 * {@code SpiderResponseContext.lastResponse()} 访问。
 */
public class ResponseContextFilter implements SpiderInvocationFilter {

    @Override
    public Object filter(SpiderInvocationContext ctx, SpiderFilterChain chain) throws Throwable {
        SpiderResponseContext.clear();
        try {
            Object result = chain.next(ctx);
            // 成功时设置响应，保留到调用方读取（下次调用入口会 clear）。
            // 不在 finally 里 clear——否则调用方执行 lastResponse() 时已被清空。
            if (ctx.response() != null) {
                SpiderResponseContext.set(ctx.response());
            }
            return result;
        } catch (Throwable t) {
            SpiderResponseContext.clear();
            throw t;
        }
    }
}
