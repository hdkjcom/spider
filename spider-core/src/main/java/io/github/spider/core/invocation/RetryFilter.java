package io.github.spider.core.invocation;

import io.github.spider.core.client.SpiderClientException;
import io.github.spider.core.exception.*;
import io.github.spider.core.metrics.SpiderMetrics;
import io.github.spider.core.runtime.SpiderRuntime;
import io.github.spider.core.transport.SpiderResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 重试 filter：在重试循环中执行剩余链，支持退避和异常类型分类。
 *
 * <p>重试策略（基于异常类型）：
 * <ul>
 *   <li>{@link SpiderConfigurationException} — 永不重试（永久性配置错误）</li>
 *   <li>{@link SpiderCircuitBreakerOpenException} — 永不重试（熔断器已开启）</li>
 *   <li>{@link SpiderRateLimitException} — 永不重试（已被限流）</li>
 *   <li>{@link SpiderHttpClientException} — 永不重试（客户端 4xx 错误）</li>
 *   <li>{@link SpiderHttpServerException} — 如果配置允许则重试（服务端 5xx）</li>
 *   <li>{@link SpiderIOException} — 如果配置允许则重试（网络故障）</li>
 *   <li>{@link SpiderServiceDiscoveryException} — 如果配置允许则重试（短暂故障）</li>
 *   <li>{@link IOException} — 如果配置允许则重试（原始网络 I/O 错误）</li>
 * </ul>
 */
public class RetryFilter implements SpiderInvocationFilter {

    private static final Logger log = LoggerFactory.getLogger(RetryFilter.class);

    private final SpiderMetrics metrics;

    public RetryFilter(SpiderMetrics metrics) {
        this.metrics = metrics != null ? metrics : SpiderMetrics.NOOP;
    }

    @Override
    public Object filter(SpiderInvocationContext ctx, SpiderFilterChain chain) throws Throwable {
        int attempts = ctx.methodMetadata().maxAttempts();
        Exception lastException = null;

        for (int i = 0; i < attempts; i++) {
            try {
                // 每次迭代必须使用全新的子链：SpiderFilterChain 内部游标单调递增，
                // 若复用同一个 chain，第二次调用 next() 时游标已到链尾会直接返回 null，
                // 传输 filter 不会再次执行，重试将永远看到上一次的失败响应。
                SpiderFilterChain attemptChain = chain.subChain();
                // 进入重试前清空上一次的响应，避免残留状态被误判为成功
                ctx.setResponse(null);
                Object result = attemptChain.next(ctx);
                // 成功
                SpiderResponse response = ctx.response();
                if (response != null && !response.isSuccessful()) {
                    int sc = response.statusCode();
                    if (sc >= 400 && sc < 500) {
                        throw new SpiderHttpClientException(sc,
                                "HTTP " + sc + " for " + ctx.request().fullUrl());
                    }
                    throw new SpiderHttpServerException(sc,
                            "HTTP " + sc + " for " + ctx.request().fullUrl());
                }
                metrics.recordSuccess(ctx.clientName(), ctx.method().getName(), ctx.request(), response);
                SpiderRuntime.getInstance().recordSuccess(ctx.clientName(), ctx.methodMetadata().httpMethod());
                if (response != null) {
                    SpiderRuntime.getInstance().recordLatency(ctx.clientName(), ctx.methodMetadata().httpMethod(), response.elapsedMillis());
                }
                log.debug("{} {} 调用成功 ({}ms)", ctx.clientName(), ctx.request().fullUrl(),
                        response != null ? response.elapsedMillis() : 0);
                return result;

            } catch (IOException e) {
                lastException = e;
                if (i < attempts - 1 && ctx.methodMetadata().isRetryable()
                        && ctx.methodMetadata().shouldRetryOn(e)) {
                    ctx.incrementRetryCount();
                    metrics.recordRetry(ctx.clientName(), ctx.method().getName(), i + 1, e);
                    SpiderRuntime.getInstance().recordRetry(ctx.clientName());
                    sleepBackoff(ctx, i + 1);
                }

            } catch (SpiderConfigurationException | SpiderCircuitBreakerOpenException
                    | SpiderRateLimitException | SpiderHttpClientException e) {
                // 这些异常永不重试
                lastException = e;
                break;

            } catch (SpiderServiceDiscoveryException | SpiderHttpServerException | SpiderIOException e) {
                // 这些异常可被重试
                lastException = e;
                if (i < attempts - 1 && ctx.methodMetadata().isRetryable()
                        && ctx.methodMetadata().shouldRetryOn(e)) {
                    ctx.incrementRetryCount();
                    metrics.recordRetry(ctx.clientName(), ctx.method().getName(), i + 1, e);
                    SpiderRuntime.getInstance().recordRetry(ctx.clientName());
                    sleepBackoff(ctx, i + 1);
                }

            } catch (SpiderClientException e) {
                // 向后兼容：通用 SpiderClientException 的处理
                if (ctx.methodMetadata().shouldIgnoreStatus(e.statusCode())) {
                    lastException = e;
                    break;
                }
                if (e.statusCode() >= 400 && e.statusCode() < 500) {
                    lastException = e;
                    break;
                }
                lastException = e;
                if (i < attempts - 1 && ctx.methodMetadata().isRetryable()
                        && ctx.methodMetadata().shouldRetryOn(e)) {
                    ctx.incrementRetryCount();
                    metrics.recordRetry(ctx.clientName(), ctx.method().getName(), i + 1, e);
                    SpiderRuntime.getInstance().recordRetry(ctx.clientName());
                    sleepBackoff(ctx, i + 1);
                }
            }
        }

        // 所有重试已耗尽
        metrics.recordFailure(ctx.clientName(), ctx.method().getName(), ctx.request(), lastException);
        SpiderRuntime.getInstance().recordFailure(ctx.clientName(), ctx.methodMetadata().httpMethod());
        SpiderRuntime.getInstance().recordError(ctx.clientName(), ctx.method().getName(),
                lastException != null ? lastException.getMessage() : "unknown");
        log.warn("{} {} 调用失败，重试{}次后放弃: {}", ctx.clientName(), ctx.request().fullUrl(),
                attempts, lastException != null ? lastException.getMessage() : "unknown");

        ctx.setLastException(lastException);
        throw lastException != null ? lastException
                : new SpiderClientException("Request failed for " + ctx.method().getName());
    }

    private long computeBackoff(SpiderInvocationContext ctx, int attempt) {
        if ("EXPONENTIAL".equalsIgnoreCase(ctx.methodMetadata().backoffStrategy())) {
            long delay = ctx.methodMetadata().backoffMillis() * (1L << (attempt - 1));
            long max = ctx.methodMetadata().maxBackoffMillis();
            return max > 0 ? Math.min(delay, max) : delay;
        }
        return ctx.methodMetadata().backoffMillis();
    }

    private void sleepBackoff(SpiderInvocationContext ctx, int attempt) {
        try {
            Thread.sleep(computeBackoff(ctx, attempt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
