package io.github.spider.core.invocation;

import io.github.spider.core.metadata.RequestTemplate;
import io.github.spider.core.transport.SpiderRequest;

/**
 * 根据方法元数据、参数和已解析的 baseUrl 构建 {@link SpiderRequest}。
 */
public class RequestBuildFilter implements SpiderInvocationFilter {

    private final RequestTemplate requestTemplate;

    public RequestBuildFilter(RequestTemplate requestTemplate) {
        this.requestTemplate = requestTemplate;
    }

    @Override
    public Object filter(SpiderInvocationContext ctx, SpiderFilterChain chain) throws Throwable {
        SpiderRequest request = requestTemplate.build(
                ctx.methodMetadata(), ctx.args(), ctx.resolvedBaseUrl());
        // 动态配置覆盖超时（由 ConfigOverrideFilter 注入）
        Object cfgTimeout = ctx.attribute("config.timeout");
        if (cfgTimeout instanceof Number) {
            request.timeoutMillis(((Number) cfgTimeout).intValue());
        }
        ctx.setRequest(request);
        return chain.next(ctx);
    }
}
