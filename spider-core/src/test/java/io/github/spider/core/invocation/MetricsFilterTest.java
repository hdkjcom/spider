package io.github.spider.core.invocation;

import io.github.spider.core.metrics.SpiderMetrics;
import io.github.spider.core.metadata.MethodMetadata;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link MetricsFilter} 的透传行为：
 * <ul>
 *   <li>chain.next 正常返回时透传结果</li>
 *   <li>chain.next 抛异常时透传异常（不吞）</li>
 * </ul>
 *
 * <p>使用计数 lambda 实现的 {@link SpiderMetrics}，验证 filter 不改变调用行为。
 */
class MetricsFilterTest {

    @Test
    void shouldPassThroughNormalResult() throws Throwable {
        AtomicInteger successCalls = new AtomicInteger();
        AtomicInteger failureCalls = new AtomicInteger();
        SpiderMetrics countingMetrics = new SpiderMetrics() {
            @Override
            public void recordSuccess(String clientName, String methodName,
                                      SpiderRequest request, SpiderResponse response) {
                successCalls.incrementAndGet();
            }
            @Override
            public void recordFailure(String clientName, String methodName,
                                      SpiderRequest request, Exception exception) {
                failureCalls.incrementAndGet();
            }
        };

        MetricsFilter filter = new MetricsFilter(countingMetrics);
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "testClient", null, null, new MethodMetadata(), "http://localhost");

        SpiderFilterChain chain = new SpiderFilterChain(
                Collections.<SpiderInvocationFilter>singletonList((c, ch) -> "success"));

        Object result = filter.filter(ctx, chain);

        assertEquals("success", result, "MetricsFilter 应透传正常结果");
        // 注意：当前 MetricsFilter 实现不主动调用 recordSuccess/recordFailure，
        // 此处验证的是 filter 不改变链行为。
    }

    @Test
    void shouldPropagateExceptionWithoutSwallowing() {
        AtomicInteger failureCalls = new AtomicInteger();
        SpiderMetrics countingMetrics = new SpiderMetrics() {
            @Override
            public void recordFailure(String clientName, String methodName,
                                      SpiderRequest request, Exception exception) {
                failureCalls.incrementAndGet();
            }
        };

        MetricsFilter filter = new MetricsFilter(countingMetrics);
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "testClient", null, null, new MethodMetadata(), "http://localhost");

        SpiderFilterChain chain = new SpiderFilterChain(
                Collections.<SpiderInvocationFilter>singletonList((c, ch) -> {
                    throw new IllegalArgumentException("downstream error");
                }));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> filter.filter(ctx, chain));
        assertEquals("downstream error", ex.getMessage(),
                "异常应原样传播，不被 MetricsFilter 吞掉");
    }

    @Test
    void shouldAcceptNoopMetrics() throws Throwable {
        MetricsFilter filter = new MetricsFilter(SpiderMetrics.NOOP);
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "testClient", null, null, new MethodMetadata(), "http://localhost");

        SpiderFilterChain chain = new SpiderFilterChain(
                Collections.<SpiderInvocationFilter>singletonList((c, ch) -> "noop-ok"));

        Object result = filter.filter(ctx, chain);

        assertEquals("noop-ok", result,
                "使用 SpiderMetrics.NOOP 时 filter 应正常工作");
    }

    @Test
    void shouldHandleNullMetricsGracefully() {
        // 构造时传入 null 应降级为 NOOP 而不抛 NPE
        MetricsFilter filter = new MetricsFilter(null);
        assertNotNull(filter, "传入 null metrics 时构造器不应抛异常");
    }
}
