package io.github.spider.core.invocation;

/**
 * 调用管道中的一个过滤器节点。
 *
 * <p>每个 filter 在调用前后执行治理逻辑，然后通过 {@code chain.next(ctx)} 将控制权交给下一个 filter。
 * 该模式与 Servlet Filter 类似：filter 可以在调用 chain.next() 之前和之后执行工作，
 * 也可以捕获异常并决定是否重试、降级或重新抛出。
 *
 * <p>实现必须是线程安全的，因为多个代理调用可能并发执行。
 */
public interface SpiderInvocationFilter {

    /**
     * 处理一次调用。
     *
     * @param ctx   当前调用上下文
     * @param chain 过滤器链，用于将控制权交给下一个 filter
     * @return 解码后的响应体，或 void 方法返回 null
     * @throws Throwable 任何 filter 或远程调用抛出的异常
     */
    Object filter(SpiderInvocationContext ctx, SpiderFilterChain chain) throws Throwable;
}
