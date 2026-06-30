package io.github.spider.core.invocation;

import io.github.spider.core.codec.SpiderDecoder;
import io.github.spider.core.exception.SpiderConfigurationException;
import io.github.spider.core.metadata.MethodMetadata;
import io.github.spider.core.transport.SpiderResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link DecodeFilter}：
 * void 返回 null、有 decoder 时调用 decode、decoder 为 null 且非 void 时抛配置异常。
 *
 * <p>decoder 用 lambda 实现；filter 在解码前会先执行剩余链（空链返回 null），
 * 解码发生在有 response 存在的分支。
 */
class DecodeFilterTest {

    private static SpiderInvocationContext ctxWithResponse(Class<?> returnType, SpiderResponse response) {
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "svc", null, null, new MethodMetadata().returnType(returnType), null);
        ctx.setResponse(response);
        return ctx;
    }

    @Test
    void voidReturnTypeReturnsNull() throws Throwable {
        SpiderDecoder decoder = (body, type) -> {
            throw new AssertionError("void 返回类型不应调用 decoder");
        };
        DecodeFilter filter = new DecodeFilter(decoder);
        SpiderInvocationContext ctx = ctxWithResponse(
                void.class,
                new SpiderResponse().statusCode(200).bodyBytes("ignored".getBytes(StandardCharsets.UTF_8))
        );

        Object result = filter.filter(ctx, new SpiderFilterChain(Collections.<SpiderInvocationFilter>emptyList()));

        assertNull(result, "void/Void 返回类型必须返回 null，不调用 decoder");
    }

    @Test
    void voidObjectReturnTypeReturnsNull() throws Throwable {
        SpiderDecoder decoder = (body, type) -> {
            throw new AssertionError("Void 返回类型不应调用 decoder");
        };
        DecodeFilter filter = new DecodeFilter(decoder);
        SpiderInvocationContext ctx = ctxWithResponse(
                Void.class,
                new SpiderResponse().statusCode(200).bodyBytes("ignored".getBytes(StandardCharsets.UTF_8))
        );

        Object result = filter.filter(ctx, new SpiderFilterChain(Collections.<SpiderInvocationFilter>emptyList()));

        assertNull(result);
    }

    @Test
    void decoderIsInvokedForNonVoidReturnType() throws Throwable {
        final AtomicInteger decodeCalls = new AtomicInteger();
        final byte[][] receivedBody = {null};
        SpiderDecoder decoder = (body, type) -> {
            decodeCalls.incrementAndGet();
            receivedBody[0] = body;
            assertEquals(String.class, type, "decoder 应收到 methodMetadata.returnType()");
            return new String(body, StandardCharsets.UTF_8);
        };
        DecodeFilter filter = new DecodeFilter(decoder);
        byte[] body = "payload".getBytes(StandardCharsets.UTF_8);
        SpiderInvocationContext ctx = ctxWithResponse(String.class, new SpiderResponse().statusCode(200).bodyBytes(body));

        Object result = filter.filter(ctx, new SpiderFilterChain(Collections.<SpiderInvocationFilter>emptyList()));

        assertEquals("payload", result);
        assertEquals(1, decodeCalls.get());
        assertArrayEquals(body, receivedBody[0]);
    }

    @Test
    void nullDecoderThrowsConfigurationException() throws Exception {
        DecodeFilter filter = new DecodeFilter(null);
        // filter 在构造异常消息时会调用 ctx.method().getName()，必须提供真实 Method
        Method method = DecodeFilterTest.class.getDeclaredMethod(
                "nullDecoderThrowsConfigurationException");
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "svc", method, null, new MethodMetadata().returnType(String.class), null);
        ctx.setResponse(new SpiderResponse().statusCode(200).bodyBytes("data".getBytes(StandardCharsets.UTF_8)));

        SpiderConfigurationException ex = assertThrows(
                SpiderConfigurationException.class,
                () -> filter.filter(ctx, new SpiderFilterChain(Collections.<SpiderInvocationFilter>emptyList()))
        );
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("nullDecoderThrowsConfigurationException"),
                "异常消息应包含方法名: " + ex.getMessage());
    }

    @Test
    void noResponseReturnsUpstreamResult() throws Throwable {
        SpiderDecoder decoder = (body, type) -> {
            throw new AssertionError("无 response 时不应调用 decoder");
        };
        DecodeFilter filter = new DecodeFilter(decoder);
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "svc", null, null, new MethodMetadata().returnType(String.class), null);
        // 无 response —— filter 应返回上游链结果（这里链尾返回 null）

        Object result = filter.filter(ctx, new SpiderFilterChain(Collections.<SpiderInvocationFilter>emptyList()));

        assertNull(result, "无 response 时应直接返回上游链结果，不进入解码分支");
    }
}
