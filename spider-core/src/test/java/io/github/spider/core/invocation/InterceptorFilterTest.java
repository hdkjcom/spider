package io.github.spider.core.invocation;

import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link InterceptorFilter}：beforeRequest / afterResponse / onError 生命周期，
 * 以及多个 interceptor 的顺序执行。
 *
 * <p>interceptor 用匿名内部类实现，下游 filter 用于设置响应或抛异常。
 */
class InterceptorFilterTest {

    @Test
    void beforeRequestModifiesRequest() throws Throwable {
        SpiderInterceptor interceptor = new SpiderInterceptor() {
            @Override
            public SpiderRequest beforeRequest(SpiderRequest request) {
                request.addHeader("X-Custom", "injected");
                return request;
            }
        };
        InterceptorFilter filter = new InterceptorFilter(Collections.singletonList(interceptor));

        SpiderRequest request = new SpiderRequest().url("http://example.com/api");
        SpiderInvocationContext ctx = new SpiderInvocationContext("svc", null, null, null, null);
        ctx.setRequest(request);

        SpiderInvocationFilter downstream = (c, chain) -> {
            assertTrue(c.request().headers().containsKey("X-Custom"),
                    "beforeRequest 应添加 X-Custom 头");
            assertEquals("injected", c.request().headers().get("X-Custom").get(0));
            return "ok";
        };

        Object result = filter.filter(ctx, new SpiderFilterChain(Collections.singletonList(downstream)));

        assertEquals("ok", result);
    }

    @Test
    void afterResponseModifiesResponse() throws Throwable {
        SpiderInterceptor interceptor = new SpiderInterceptor() {
            @Override
            public SpiderResponse afterResponse(SpiderResponse response) {
                response.addHeader("X-Processed", "true");
                return response;
            }
        };
        InterceptorFilter filter = new InterceptorFilter(Collections.singletonList(interceptor));

        SpiderRequest request = new SpiderRequest();
        SpiderInvocationContext ctx = new SpiderInvocationContext("svc", null, null, null, null);
        ctx.setRequest(request);

        SpiderInvocationFilter downstream = (c, chain) -> {
            c.setResponse(new SpiderResponse().statusCode(200));
            return "done";
        };

        Object result = filter.filter(ctx, new SpiderFilterChain(Collections.singletonList(downstream)));

        assertEquals("done", result);
        assertNotNull(ctx.response(), "afterResponse 执行后 response 不应为 null");
        assertTrue(ctx.response().headers().containsKey("X-Processed"),
                "afterResponse 应添加 X-Processed 头");
    }

    @Test
    void onErrorSwallowsExceptionWhenReturnsTrue() throws Throwable {
        SpiderInterceptor interceptor = new SpiderInterceptor() {
            @Override
            public boolean onError(SpiderRequest request, Exception ex) {
                return true; // 吞掉异常
            }
        };
        InterceptorFilter filter = new InterceptorFilter(Collections.singletonList(interceptor));

        SpiderRequest request = new SpiderRequest();
        SpiderInvocationContext ctx = new SpiderInvocationContext("svc", null, null, null, null);
        ctx.setRequest(request);

        SpiderInvocationFilter downstream = (c, chain) -> {
            throw new RuntimeException("模拟传输异常");
        };

        Object result = filter.filter(ctx, new SpiderFilterChain(Collections.singletonList(downstream)));

        assertNull(result, "onError 返回 true 时应吞掉异常并返回 null");
    }

    @Test
    void onErrorRethrowsWhenReturnsFalse() throws Throwable {
        SpiderInterceptor interceptor = new SpiderInterceptor() {
            @Override
            public boolean onError(SpiderRequest request, Exception ex) {
                return false; // 不吞异常
            }
        };
        InterceptorFilter filter = new InterceptorFilter(Collections.singletonList(interceptor));

        SpiderRequest request = new SpiderRequest();
        SpiderInvocationContext ctx = new SpiderInvocationContext("svc", null, null, null, null);
        ctx.setRequest(request);

        SpiderInvocationFilter downstream = (c, chain) -> {
            throw new RuntimeException("模拟传输异常");
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> filter.filter(ctx, new SpiderFilterChain(Collections.singletonList(downstream))));
        assertEquals("模拟传输异常", ex.getMessage(),
                "onError 返回 false 时应重新抛出原始异常");
    }

    @Test
    void multipleInterceptorsExecuteBeforeRequestInOrder() throws Throwable {
        List<String> trace = new ArrayList<>();

        SpiderInterceptor interceptorA = new SpiderInterceptor() {
            @Override
            public SpiderRequest beforeRequest(SpiderRequest request) {
                trace.add("A:before");
                return request;
            }
        };
        SpiderInterceptor interceptorB = new SpiderInterceptor() {
            @Override
            public SpiderRequest beforeRequest(SpiderRequest request) {
                trace.add("B:before");
                return request;
            }
        };
        SpiderInterceptor interceptorC = new SpiderInterceptor() {
            @Override
            public SpiderRequest beforeRequest(SpiderRequest request) {
                trace.add("C:before");
                return request;
            }
        };

        InterceptorFilter filter = new InterceptorFilter(
                Arrays.asList(interceptorA, interceptorB, interceptorC));

        SpiderRequest request = new SpiderRequest();
        SpiderInvocationContext ctx = new SpiderInvocationContext("svc", null, null, null, null);
        ctx.setRequest(request);

        SpiderInvocationFilter downstream = (c, chain) -> {
            trace.add("downstream");
            return "ok";
        };

        filter.filter(ctx, new SpiderFilterChain(Collections.singletonList(downstream)));

        assertEquals(Arrays.asList("A:before", "B:before", "C:before", "downstream"),
                trace, "beforeRequest 应按 interceptor 注册顺序执行");
    }

    @Test
    void afterResponseExecutesInSameOrder() throws Throwable {
        List<String> trace = new ArrayList<>();

        SpiderInterceptor interceptorA = new SpiderInterceptor() {
            @Override
            public SpiderResponse afterResponse(SpiderResponse response) {
                trace.add("A:after");
                return response;
            }
        };
        SpiderInterceptor interceptorB = new SpiderInterceptor() {
            @Override
            public SpiderResponse afterResponse(SpiderResponse response) {
                trace.add("B:after");
                return response;
            }
        };

        InterceptorFilter filter = new InterceptorFilter(
                Arrays.asList(interceptorA, interceptorB));

        SpiderRequest request = new SpiderRequest();
        SpiderInvocationContext ctx = new SpiderInvocationContext("svc", null, null, null, null);
        ctx.setRequest(request);

        SpiderInvocationFilter downstream = (c, chain) -> {
            c.setResponse(new SpiderResponse().statusCode(200));
            trace.add("downstream");
            return "ok";
        };

        filter.filter(ctx, new SpiderFilterChain(Collections.singletonList(downstream)));

        assertEquals(Arrays.asList("downstream", "A:after", "B:after"),
                trace, "afterResponse 应按 interceptor 注册顺序执行（下游先返回，再依次回调）");
    }
}
