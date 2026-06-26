package io.github.spider.core.invocation;

import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 包装现有 {@link SpiderInterceptor} 列表，按生命周期调用。
 *
 * <p>执行顺序：
 * <ol>
 *   <li>调用所有 interceptor 的 {@code beforeRequest}</li>
 *   <li>调用下一个 filter</li>
 *   <li>成功时调用所有 interceptor 的 {@code afterResponse}</li>
 *   <li>失败时调用所有 interceptor 的 {@code onError}（如果任一 onError 返回 true 则吞掉异常）</li>
 * </ol>
 */
public class InterceptorFilter implements SpiderInvocationFilter {

    private final List<SpiderInterceptor> interceptors;

    public InterceptorFilter(List<SpiderInterceptor> interceptors) {
        this.interceptors = new ArrayList<>(interceptors);
    }

    @Override
    public Object filter(SpiderInvocationContext ctx, SpiderFilterChain chain) throws Throwable {
        SpiderRequest request = ctx.request();

        // before
        for (SpiderInterceptor interceptor : interceptors) {
            request = interceptor.beforeRequest(request);
        }
        ctx.setRequest(request);

        try {
            Object result = chain.next(ctx);

            // after
            SpiderResponse response = ctx.response();
            if (response != null) {
                for (SpiderInterceptor interceptor : interceptors) {
                    response = interceptor.afterResponse(response);
                }
                ctx.setResponse(response);
            }
            return result;

        } catch (Throwable t) {
            // error notification
            Exception ex = t instanceof Exception ? (Exception) t : new RuntimeException(t);
            for (SpiderInterceptor interceptor : interceptors) {
                if (interceptor.onError(ctx.request(), ex)) {
                    return null; // interceptor 吞掉异常
                }
            }
            throw t;
        }
    }
}
