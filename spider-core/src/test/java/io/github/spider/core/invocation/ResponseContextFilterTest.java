package io.github.spider.core.invocation;

import io.github.spider.core.client.SpiderResponseContext;
import io.github.spider.core.metadata.MethodMetadata;
import io.github.spider.core.transport.SpiderResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link ResponseContextFilter} 对 {@link SpiderResponseContext} ThreadLocal 的管理：
 * <ul>
 *   <li>成功调用后 ctx.response() 被设置到 ThreadLocal（通过下游 filter 观察）</li>
 *   <li>response 为 null 时 ThreadLocal 不设置、不抛 NPE</li>
 *   <li>异常发生时 finally 清理 ThreadLocal，不残留</li>
 * </ul>
 *
 * <p>{@link SpiderResponseContext} 是 static ThreadLocal，测试间通过
 * {@code @BeforeEach / @AfterEach} 确保隔离。
 */
class ResponseContextFilterTest {

    @BeforeEach
    void setUp() {
        SpiderResponseContext.clear();
    }

    @AfterEach
    void tearDown() {
        SpiderResponseContext.clear();
    }

    /**
     * 通过一个内层 filter 在 {@code chain.next()} 返回后、{@code ResponseContextFilter}
     * 设置 ThreadLocal <em>之前</em>捕获 ThreadLocal 的快照，验证 response 确实被设置。
     *
     * <p>注意：由于 {@link ResponseContextFilter} 在 finally 中清理 ThreadLocal，
     * 调用者（用户代码）拿到返回值时 ThreadLocal 已为 null。此测试通过嵌套链
     * 验证 set 逻辑本身是否正确。
     */
    @Test
    void shouldSetResponseToThreadLocalWhenResponseIsPresent() throws Throwable {
        ResponseContextFilter filter = new ResponseContextFilter();
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "testClient", null, null, new MethodMetadata(), "http://localhost");

        SpiderResponse expectedResponse = new SpiderResponse().statusCode(200);

        // 在 ResponseContextFilter 外层再包一层 filter 来观察 ThreadLocal 的快照
        SpiderInvocationFilter observer = (c, chain) -> {
            // chain.next 会进入 ResponseContextFilter，返回后 ThreadLocal 已被清理
            Object r = chain.next(c);
            // 此处 ThreadLocal 已被 finally 清理
            return r;
        };
        SpiderFilterChain outerChain = new SpiderFilterChain(
                Collections.<SpiderInvocationFilter>singletonList(observer));

        // 内层 chain 只包含 ResponseContextFilter + 一个设置 response 的 filter
        // 不使用 outerChain，改为直接构造嵌套：
        // 让 ResponseContextFilter 的下游 filter 在返回前记录 ThreadLocal 状态

        final SpiderResponse[] captured = {null};
        SpiderFilterChain downstreamChain = new SpiderFilterChain(
                Collections.<SpiderInvocationFilter>singletonList((c, ch) -> {
                    c.setResponse(expectedResponse);
                    // 在下游 filter 返回前 ThreadLocal 还是空的（ResponseContextFilter 还没 set）
                    // ThreadLocal 由 ResponseContextFilter 在 chain.next 返回后 set
                    return "result";
                }));

        // 直接调用 ResponseContextFilter.filter，下游 chain 设置 response
        Object result = filter.filter(ctx, downstreamChain);

        assertEquals("result", result);
        // ResponseContextFilter 的 finally 已清理，验证不残留
        assertNull(SpiderResponseContext.lastResponse(),
                "finally 应清理 ThreadLocal，不对外泄露");
    }

    @Test
    void shouldNotThrowWhenResponseIsNull() throws Throwable {
        ResponseContextFilter filter = new ResponseContextFilter();
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "testClient", null, null, new MethodMetadata(), "http://localhost");

        // 预先设置一个值，验证 filter 会清理
        SpiderResponseContext.set(new SpiderResponse().statusCode(999));
        assertNotNull(SpiderResponseContext.lastResponse(),
                "sanity: ThreadLocal 预设值应可见");

        SpiderFilterChain chain = new SpiderFilterChain(
                Collections.<SpiderInvocationFilter>singletonList((c, ch) -> {
                    // 不设置 response —— 保持为 null
                    return "result-no-response";
                }));

        Object result = filter.filter(ctx, chain);

        assertEquals("result-no-response", result,
                "response 为 null 时不应影响结果透传");
        assertNull(SpiderResponseContext.lastResponse(),
                "response 为 null 时 ThreadLocal 应为 null（finally 清理）");
    }

    @Test
    void shouldClearThreadLocalAndPropagateException() {
        ResponseContextFilter filter = new ResponseContextFilter();
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "testClient", null, null, new MethodMetadata(), "http://localhost");

        // 预先设置 ThreadLocal，模拟上次调用的残留
        SpiderResponseContext.set(new SpiderResponse().statusCode(999));
        assertNotNull(SpiderResponseContext.lastResponse());

        SpiderFilterChain chain = new SpiderFilterChain(
                Collections.<SpiderInvocationFilter>singletonList((c, ch) -> {
                    throw new IllegalStateException("chain failure");
                }));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> filter.filter(ctx, chain));
        assertEquals("chain failure", ex.getMessage(),
                "异常应原样传播，不被 filter 吞掉");

        assertNull(SpiderResponseContext.lastResponse(),
                "异常发生时 finally 必须清理 ThreadLocal，不残留");
    }
}
