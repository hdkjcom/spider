package io.github.spider.core.invocation;

import io.github.spider.core.exception.SpiderConfigurationException;
import io.github.spider.core.codec.SpiderDecoder;

/**
 * 将响应 body 解码为返回类型。
 *
 * <p>处理 void、String、null body 和泛型返回类型。
 */
public class DecodeFilter implements SpiderInvocationFilter {

    private final SpiderDecoder decoder;

    public DecodeFilter(SpiderDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public Object filter(SpiderInvocationContext ctx, SpiderFilterChain chain) throws Throwable {
        // 先执行剩余链（如有）
        Object result = chain.next(ctx);

        // 解码逻辑：仅当有响应时处理
        if (ctx.response() != null) {
            if (ctx.methodMetadata().returnType() == void.class
                    || ctx.methodMetadata().returnType() == Void.class) {
                return null;
            }
            if (decoder == null) {
                throw new SpiderConfigurationException("No SpiderDecoder configured, cannot decode response for "
                        + ctx.method().getName());
            }
            return decoder.decode(ctx.response().bodyBytes(), ctx.methodMetadata().returnType());
        }
        return result;
    }
}
