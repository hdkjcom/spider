package io.github.spider.core.invocation;

import io.github.spider.core.policy.FallbackFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link FallbackFilter}：正常透传、异常降级、FallbackFactory 接收异常原因。
 *
 * <p>降级实例和 FallbackFactory 用匿名内部类实现，Method 通过反射获取。
 */
class FallbackFilterTest {

    /** 测试用 API，用于反射获取 Method 并定义降级逻辑。 */
    interface TestApi {
        String call(String param);
    }

    /** TestApi 的降级实现。 */
    static class TestApiFallback implements TestApi {
        @Override
        public String call(String param) {
            return "fallback-" + param;
        }
    }

    @Test
    void chainSucceedsNoFallbackTriggered() throws Throwable {
        Method method = TestApi.class.getDeclaredMethod("call", String.class);
        Map<Method, Object> fallbackMap = Collections.singletonMap(
                method, new TestApiFallback());
        FallbackFilter filter = new FallbackFilter(fallbackMap, null);

        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "test-client", method, new Object[]{"hello"}, null, null);

        SpiderInvocationFilter downstream = (c, chain) -> "normal-result";

        Object result = filter.filter(ctx, new SpiderFilterChain(Collections.singletonList(downstream)));

        assertEquals("normal-result", result,
                "链正常执行时应透传下游结果，不触发降级");
    }

    @Test
    void chainThrowsFallbackInstanceIsInvoked() throws Throwable {
        Method method = TestApi.class.getDeclaredMethod("call", String.class);
        Map<Method, Object> fallbackMap = Collections.singletonMap(
                method, new TestApiFallback());
        FallbackFilter filter = new FallbackFilter(fallbackMap, null);

        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "test-client", method, new Object[]{"world"}, null, null);

        SpiderInvocationFilter downstream = (c, chain) -> {
            throw new RuntimeException("远程调用失败");
        };

        Object result = filter.filter(ctx, new SpiderFilterChain(Collections.singletonList(downstream)));

        assertEquals("fallback-world", result,
                "链抛异常时应调用降级实例的同名方法并返回其结果");
    }

    @Test
    void fallbackFactoryReceivesExceptionCause() throws Throwable {
        Method method = TestApi.class.getDeclaredMethod("call", String.class);
        final AtomicReference<Throwable> capturedCause = new AtomicReference<>();

        FallbackFactory<TestApi> factory = cause -> {
            capturedCause.set(cause);
            return new TestApiFallback();
        };

        Map<Method, Object> fallbackMap = Collections.singletonMap(method, (Object) factory);
        FallbackFilter filter = new FallbackFilter(fallbackMap, null);

        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "test-client", method, new Object[]{"x"}, null, null);

        SpiderInvocationFilter downstream = (c, chain) -> {
            throw new IllegalStateException("连接超时");
        };

        Object result = filter.filter(ctx, new SpiderFilterChain(Collections.singletonList(downstream)));

        assertEquals("fallback-x", result);
        assertNotNull(capturedCause.get(), "FallbackFactory.create() 应被调用");
        assertTrue(capturedCause.get() instanceof IllegalStateException,
                "FallbackFactory 应收到原始异常");
        assertEquals("连接超时", capturedCause.get().getMessage());
    }

    @Test
    void noFallbackRegisteredRethrowsOriginalException() throws Throwable {
        Method method = TestApi.class.getDeclaredMethod("call", String.class);
        FallbackFilter filter = new FallbackFilter(Collections.<Method, Object>emptyMap(), null);

        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "test-client", method, new Object[]{"x"}, null, null);

        SpiderInvocationFilter downstream = (c, chain) -> {
            throw new RuntimeException("无降级配置");
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> filter.filter(ctx, new SpiderFilterChain(Collections.singletonList(downstream))));
        assertEquals("无降级配置", ex.getMessage(),
                "fallbackMap 为空时应重新抛出原始异常");
    }
}
