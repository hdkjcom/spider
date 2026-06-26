package io.github.spider.core.invocation;

import io.github.spider.core.transport.SpiderResponse;
import io.github.spider.core.transport.SpiderTransport;

/**
 * 执行实际的远程传输调用。
 */
public class TransportFilter implements SpiderInvocationFilter {

    private final SpiderTransport transport;

    public TransportFilter(SpiderTransport transport) {
        this.transport = transport;
    }

    @Override
    public Object filter(SpiderInvocationContext ctx, SpiderFilterChain chain) throws Throwable {
        SpiderResponse response = transport.execute(ctx.request());
        ctx.setResponse(response);
        return chain.next(ctx);
    }
}
