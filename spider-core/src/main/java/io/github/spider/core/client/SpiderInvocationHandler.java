package io.github.spider.core.client;

import io.github.spider.core.codec.SpiderDecoder;
import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.metadata.MethodMetadata;
import io.github.spider.core.metrics.SpiderMetrics;
import io.github.spider.core.metadata.RequestTemplate;
import io.github.spider.core.policy.FallbackFactory;
import io.github.spider.core.runtime.SpiderRuntime;
import io.github.spider.core.transport.SpiderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.spider.core.transport.SpiderResponse;
import io.github.spider.core.transport.SpiderTransport;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * JDK Dynamic Proxy InvocationHandler.
 * Drives the full request lifecycle for each method call on a @SpiderClient interface.
 */
public class SpiderInvocationHandler implements InvocationHandler {

    private static final Logger log = LoggerFactory.getLogger(SpiderInvocationHandler.class);

    private final String clientName;
    private final String baseUrl;
    private final Map<Method, MethodMetadata> metadataCache;
    private final RequestTemplate requestTemplate;
    private final SpiderTransport transport;
    private final SpiderDecoder decoder;
    private final List<SpiderInterceptor> interceptors;
    private final Map<Method, Object> fallbackMap;
    private final SpiderMetrics metrics;

    public SpiderInvocationHandler(String clientName,
                                   String baseUrl,
                                   Map<Method, MethodMetadata> metadataCache,
                                   RequestTemplate requestTemplate,
                                   SpiderTransport transport,
                                   SpiderDecoder decoder,
                                   List<SpiderInterceptor> interceptors,
                                   Map<Method, Object> fallbackMap,
                                   SpiderMetrics metrics) {
        this.clientName = clientName;
        this.baseUrl = baseUrl;
        this.metadataCache = metadataCache;
        this.requestTemplate = requestTemplate;
        this.transport = transport;
        this.decoder = decoder;
        this.interceptors = interceptors;
        this.fallbackMap = fallbackMap;
        this.metrics = metrics != null ? metrics : SpiderMetrics.NOOP;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Handle Object methods
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        MethodMetadata meta = metadataCache.get(method);
        if (meta == null) {
            throw new SpiderClientException("No Spider metadata found for method: " + method.getName()
                    + ". Is @SpiderGet or @SpiderPost missing?");
        }

        SpiderRequest request = requestTemplate.build(meta, args, baseUrl);
        SpiderResponseContext.clear();
        String methodName = method.getName();

        // Before interceptors
        for (SpiderInterceptor interceptor : interceptors) {
            request = interceptor.beforeRequest(request);
        }

        Exception lastException = null;
        int attempts = meta.maxAttempts();

        for (int i = 0; i < attempts; i++) {
            try {
                SpiderResponse response = transport.execute(request);

                // After interceptors
                for (SpiderInterceptor interceptor : interceptors) {
                    response = interceptor.afterResponse(response);
                }

                if (!response.isSuccessful()) {
                    throw new SpiderClientException(response.statusCode(),
                            "HTTP " + response.statusCode() + " for " + request.fullUrl());
                }

                // Success
                SpiderResponseContext.set(response);
                metrics.recordSuccess(clientName, methodName, request, response);
                SpiderRuntime.getInstance().recordSuccess(clientName);
                SpiderRuntime.getInstance().recordLatency(clientName, response.elapsedMillis());
                log.debug("{} {} -> 200 ({}ms)", clientName, request.fullUrl(), response.elapsedMillis());

                // Decode response
                if (meta.returnType() == void.class || meta.returnType() == Void.class) {
                    return null;
                }
                if (decoder == null) {
                    throw new SpiderClientException("No SpiderDecoder configured, cannot decode response for "
                            + method.getName());
                }
                return decoder.decode(response.bodyBytes(), meta.returnType());

            } catch (IOException e) {
                lastException = e;
                if (i < attempts - 1 && meta.isRetryable() && meta.shouldRetryOn(e)) {
                    metrics.recordRetry(clientName, methodName, i + 1, e);
                    SpiderRuntime.getInstance().recordRetry(clientName);
                    Thread.sleep(computeBackoff(meta, i + 1));
                }
            } catch (SpiderClientException e) {
                // check ignoreStatus
                if (meta.shouldIgnoreStatus(e.statusCode())) {
                    lastException = e;
                    break;
                }
                // 4xx: do not retry
                if (e.statusCode() >= 400 && e.statusCode() < 500) {
                    lastException = e;
                    break;
                }
                // 5xx: retry if configured
                lastException = e;
                if (i < attempts - 1 && meta.isRetryable() && meta.shouldRetryOn(e)) {
                    metrics.recordRetry(clientName, methodName, i + 1, e);
                    SpiderRuntime.getInstance().recordRetry(clientName);
                    Thread.sleep(computeBackoff(meta, i + 1));
                }
            }
        }

        // Record failure
        metrics.recordFailure(clientName, methodName, request, lastException);
        SpiderRuntime.getInstance().recordFailure(clientName);
        SpiderRuntime.getInstance().recordError(clientName, methodName,
                lastException != null ? lastException.getMessage() : "unknown");
        log.warn("{} {} -> FAILED after {} attempts: {}", clientName, request.fullUrl(), attempts,
                lastException != null ? lastException.getMessage() : "unknown");

        // Notify interceptors of error
        for (SpiderInterceptor interceptor : interceptors) {
            if (interceptor.onError(request, lastException)) {
                return null;
            }
        }

        // Try fallback
        Object fallbackOrFactory = fallbackMap.get(method);
        if (fallbackOrFactory != null) {
            try {
                metrics.recordFallback(clientName, methodName);
                SpiderRuntime.getInstance().recordFallback(clientName);
                log.info("{} {} -> FALLBACK triggered", clientName, request.fullUrl());
                Object fallbackInstance = resolveFallback(fallbackOrFactory, lastException);
                return method.invoke(fallbackInstance, args);
            } catch (Exception fbEx) {
                throw new SpiderClientException("Fallback failed for " + method.getName(), fbEx);
            }
        }

        throw lastException != null ? lastException : new SpiderClientException("Request failed for " + method.getName());
    }

    @SuppressWarnings("unchecked")
    private Object resolveFallback(Object fallbackOrFactory, Throwable cause) throws Exception {
        if (fallbackOrFactory instanceof FallbackFactory) {
            return ((FallbackFactory<Object>) fallbackOrFactory).create(cause);
        }
        return fallbackOrFactory;
    }

    private long computeBackoff(MethodMetadata meta, int attempt) {
        if ("EXPONENTIAL".equalsIgnoreCase(meta.backoffStrategy())) {
            long delay = meta.backoffMillis() * (1L << (attempt - 1));
            long max = meta.maxBackoffMillis();
            return max > 0 ? Math.min(delay, max) : delay;
        }
        return meta.backoffMillis();
    }
}
