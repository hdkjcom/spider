package io.github.spider.core.invocation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link SpiderFilterChain} 的链机制：游标推进、执行顺序、子链重建。
 *
 * <p>filter 用 lambda 实现 {@link SpiderInvocationFilter}，counting/sequencing filter
 * 用于验证调用次序。上下文使用 null 字段占位（被测的链机制不读取这些字段）。
 */
class SpiderFilterChainTest {

    /** 一个把自身标记写入 list 的 filter，用于断言执行顺序。 */
    private static SpiderInvocationFilter marker(String tag, List<String> trace) {
        return (ctx, chain) -> {
            trace.add("pre:" + tag);
            Object result = chain.next(ctx);
            trace.add("post:" + tag);
            return result;
        };
    }

    @Test
    void emptyChainReturnsNull() throws Throwable {
        SpiderFilterChain chain = new SpiderFilterChain(Collections.<SpiderInvocationFilter>emptyList());
        SpiderInvocationContext ctx = new SpiderInvocationContext("svc", null, null, null, null);

        Object result = chain.next(ctx);

        assertNull(result, "空链 next() 必须返回 null");
        assertEquals(0, chain.size());
    }

    @Test
    void singleFilterChainExecutes() throws Throwable {
        SpiderInvocationContext ctx = new SpiderInvocationContext("svc", null, null, null, null);
        List<String> trace = new ArrayList<>();
        SpiderFilterChain chain = new SpiderFilterChain(Collections.singletonList(
                marker("only", trace)
        ));

        Object result = chain.next(ctx);

        assertNull(result, "末尾 next() 返回 null");
        assertEquals(Arrays.asList("pre:only", "post:only"), trace);
    }

    @Test
    void multipleFiltersExecuteInOrder() throws Throwable {
        SpiderInvocationContext ctx = new SpiderInvocationContext("svc", null, null, null, null);
        List<String> trace = new ArrayList<>();
        SpiderFilterChain chain = new SpiderFilterChain(Arrays.asList(
                marker("A", trace),
                marker("B", trace),
                marker("C", trace)
        ));

        Object result = chain.next(ctx);

        assertNull(result);
        // 三层 pre/post 嵌套，证明按 A -> B -> C -> 返回 -> C -> B -> A 顺序执行
        assertEquals(Arrays.asList(
                "pre:A", "pre:B", "pre:C",
                "post:C", "post:B", "post:A"
        ), trace);
    }

    /**
     * subChain() 必须生成全新游标的链，使其能再次执行剩余 filter。
     * RetryFilter 依赖此语义在循环中重放下游，否则第二次 next() 会直接返回 null。
     */
    @Test
    void subChainReExecutesRemainingFilters() throws Throwable {
        final AtomicInteger downstreamCalls = new AtomicInteger();
        SpiderInvocationFilter downstream = (ctx, chain) -> {
            downstreamCalls.incrementAndGet();
            return chain.next(ctx);
        };
        // 一个模拟重试 filter：把下游切出子链，执行两次
        SpiderInvocationFilter retryFilter = (ctx, chain) -> {
            SpiderFilterChain sub1 = chain.subChain();
            sub1.next(ctx);
            SpiderFilterChain sub2 = chain.subChain();
            sub2.next(ctx);
            return "retry-done";
        };

        List<SpiderInvocationFilter> filters = new ArrayList<>();
        filters.add(retryFilter);
        filters.add(downstream);
        SpiderFilterChain chain = new SpiderFilterChain(filters);
        SpiderInvocationContext ctx = new SpiderInvocationContext("svc", null, null, null, null);

        Object result = chain.next(ctx);

        assertEquals("retry-done", result);
        assertEquals(2, downstreamCalls.get(),
                "subChain() 必须重新执行下游 filter 两次，而不是在第二次直接返回 null");
    }

    @Test
    void chainIsImmutableAndDefensivelyCopied() {
        List<SpiderInvocationFilter> src = new ArrayList<>();
        src.add((ctx, chain) -> chain.next(ctx));
        SpiderFilterChain chain = new SpiderFilterChain(src);

        // 改动源 list 不应影响链
        src.clear();

        assertEquals(1, chain.size(), "SpiderFilterChain 必须对入参 list 做防御性复制");
        assertNotNull(chain.filters());
        assertEquals(1, chain.filters().size());
    }
}
