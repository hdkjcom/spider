package io.github.spider.core.invocation;

import io.github.spider.core.metadata.MethodMetadata;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一次远程调用的完整上下文，在 filter chain 各节点间传递。
 *
 * <p>输入字段由 handler 设置，中间状态由各 filter 在执行过程中填充。
 */
public class SpiderInvocationContext {

    // ── 输入（handler 在启动 chain 前设置） ──
    private final String clientName;
    private final Method method;
    private final Object[] args;
    private final MethodMetadata methodMetadata;
    private final String baseUrl;

    // ── 中间状态（filter 在执行中设置） ──
    private String resolvedBaseUrl;
    private SpiderRequest request;
    private SpiderResponse response;
    private int retryCount;
    private long startNanos;
    private Exception lastException;

    // ── 输出 ──
    private Object result;

    // ── 扩展属性 ──
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    public SpiderInvocationContext(String clientName, Method method, Object[] args,
                                   MethodMetadata methodMetadata, String baseUrl) {
        this.clientName = clientName;
        this.method = method;
        this.args = args;
        this.methodMetadata = methodMetadata;
        this.baseUrl = baseUrl;
    }

    // ── getters / setters ──

    public String clientName() { return clientName; }
    public Method method() { return method; }
    public Object[] args() { return args; }
    public MethodMetadata methodMetadata() { return methodMetadata; }
    public String baseUrl() { return baseUrl; }

    public String resolvedBaseUrl() { return resolvedBaseUrl; }
    public void setResolvedBaseUrl(String v) { this.resolvedBaseUrl = v; }

    public SpiderRequest request() { return request; }
    public void setRequest(SpiderRequest v) { this.request = v; }

    public SpiderResponse response() { return response; }
    public void setResponse(SpiderResponse v) { this.response = v; }

    public int retryCount() { return retryCount; }
    public void setRetryCount(int v) { this.retryCount = v; }
    public void incrementRetryCount() { this.retryCount++; }

    public long startNanos() { return startNanos; }
    public void setStartNanos(long v) { this.startNanos = v; }

    public Exception lastException() { return lastException; }
    public void setLastException(Exception v) { this.lastException = v; }

    public Object result() { return result; }
    public void setResult(Object v) { this.result = v; }

    public Map<String, Object> attributes() { return attributes; }
    public Object attribute(String key) { return attributes.get(key); }
    public void setAttribute(String key, Object value) { attributes.put(key, value); }

    /** 调用耗时（纳秒），在 startNanos 设置后可用。 */
    public long elapsedNanos() {
        return startNanos > 0 ? System.nanoTime() - startNanos : 0;
    }
}
