package io.github.spider.core.invocation;

import io.github.spider.core.metadata.MethodMetadata;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link RetryFilter#computeBackoff} 的退避计算逻辑：
 * <ul>
 *   <li>{@code jitter=false} 时退避值固定且等于基准值</li>
 *   <li>{@code jitter=true} 时退避值落在计算值的 [50%, 150%) 区间</li>
 * </ul>
 *
 * <p>通过反射调用 private {@code computeBackoff}，仅验证数学逻辑，不进入真实睡眠。
 */
class RetryBackoffTest {

    /** 构造一个挂载给定 metadata 的 RetryFilter，并反射调用 computeBackoff。 */
    private long callComputeBackoff(MethodMetadata meta, int attempt) throws Exception {
        RetryFilter filter = new RetryFilter(null);
        SpiderInvocationContext ctx = new SpiderInvocationContext("svc", null, null, meta, "http://localhost");
        Method m = RetryFilter.class.getDeclaredMethod("computeBackoff", SpiderInvocationContext.class, int.class);
        m.setAccessible(true);
        return (long) m.invoke(filter, ctx, attempt);
    }

    @Test
    void fixedBackoffWithoutJitterIsConstant() throws Exception {
        MethodMetadata meta = new MethodMetadata()
                .maxAttempts(3)
                .backoffMillis(100)
                .backoffStrategy("FIXED")
                .jitter(false);

        long d1 = callComputeBackoff(meta, 1);
        long d2 = callComputeBackoff(meta, 2);
        long d3 = callComputeBackoff(meta, 3);

        assertEquals(100, d1, "jitter 关闭时 FIXED 退避应等于 backoffMillis");
        assertEquals(100, d2);
        assertEquals(100, d3);
    }

    @Test
    void exponentialBackoffWithoutJitterFollowsPowersOfTwo() throws Exception {
        MethodMetadata meta = new MethodMetadata()
                .maxAttempts(4)
                .backoffMillis(100)
                .backoffStrategy("EXPONENTIAL")
                .maxBackoffMillis(10_000)
                .jitter(false);

        assertEquals(100, callComputeBackoff(meta, 1), "100 * 2^0");
        assertEquals(200, callComputeBackoff(meta, 2), "100 * 2^1");
        assertEquals(400, callComputeBackoff(meta, 3), "100 * 2^2");
        assertEquals(800, callComputeBackoff(meta, 4), "100 * 2^3");
    }

    @Test
    void jitterStaysWithin50To150Percent() throws Exception {
        MethodMetadata meta = new MethodMetadata()
                .maxAttempts(5)
                .backoffMillis(100)
                .backoffStrategy("FIXED")
                .jitter(true);

        long base = 100;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (int i = 0; i < 100; i++) {
            long delay = callComputeBackoff(meta, 1);
            // 区间为 [50, 150)
            assertTrue(delay >= base * 0.5, "抖动后 delay 不应低于 50%: got " + delay);
            assertTrue(delay < base * 1.5, "抖动后 delay 不应达到 150%: got " + delay);
            if (delay < min) {
                min = delay;
            }
            if (delay > max) {
                max = delay;
            }
        }
        // 边界 sanity：100 次取样应能覆盖一段非平凡区间
        assertTrue(min < max, "100 次抖动应产生不同的退避值，否则随机性失效");
    }

    @Test
    void jitterOnExponentialAlsoBounded() throws Exception {
        MethodMetadata meta = new MethodMetadata()
                .maxAttempts(3)
                .backoffMillis(100)
                .backoffStrategy("EXPONENTIAL")
                .maxBackoffMillis(10_000)
                .jitter(true);

        // attempt=3 -> base delay = 100 * 2^2 = 400，抖动区间 [200, 600)
        long base = 400;
        for (int i = 0; i < 100; i++) {
            long delay = callComputeBackoff(meta, 3);
            assertTrue(delay >= base * 0.5, "EXP 抖动下界: got " + delay);
            assertTrue(delay < base * 1.5, "EXP 抖动上界: got " + delay);
        }
    }
}
