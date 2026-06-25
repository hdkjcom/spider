package io.github.spider.core.metadata;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds parsed metadata for a single method on a @SpiderClient interface.
 * Built from method-level annotations (@SpiderGet, @Timeout, @Retry, etc.)
 * and parameter-level annotations (@Path, @Query, @Header, @Body).
 */
public class MethodMetadata {

    private String httpMethod;
    private String pathTemplate;
    private List<ParamBinding> paramBindings = new ArrayList<>();
    private int timeoutMillis = -1;   // -1 表示使用客户端/传输层默认值
    private int maxAttempts = 1;      // 默认 1，即不重试
    private long backoffMillis = 100;
    private String backoffStrategy = "FIXED";
    private long maxBackoffMillis = 5000;
    private Set<Class<? extends Throwable>> retryOn = new HashSet<>();
    private Set<Integer> ignoreStatus = new HashSet<>();
    private Type returnType;

    public MethodMetadata() {}

    // --- Fluent setters ---

    public MethodMetadata httpMethod(String httpMethod) { this.httpMethod = httpMethod; return this; }
    public MethodMetadata pathTemplate(String pathTemplate) { this.pathTemplate = pathTemplate; return this; }
    public MethodMetadata timeoutMillis(int timeoutMillis) { this.timeoutMillis = timeoutMillis; return this; }
    public MethodMetadata maxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; return this; }
    public MethodMetadata backoffMillis(long backoffMillis) { this.backoffMillis = backoffMillis; return this; }
    public MethodMetadata backoffStrategy(String backoffStrategy) { this.backoffStrategy = backoffStrategy; return this; }
    public MethodMetadata maxBackoffMillis(long maxBackoffMillis) { this.maxBackoffMillis = maxBackoffMillis; return this; }
    public MethodMetadata addRetryOn(Class<? extends Throwable> ex) { this.retryOn.add(ex); return this; }
    public MethodMetadata addIgnoreStatus(int status) { this.ignoreStatus.add(status); return this; }
    public MethodMetadata returnType(Type returnType) { this.returnType = returnType; return this; }

    public MethodMetadata addParamBinding(ParamBinding binding) {
        this.paramBindings.add(binding);
        return this;
    }

    // --- Getters ---

    public String httpMethod() { return httpMethod; }
    public String pathTemplate() { return pathTemplate; }
    public List<ParamBinding> paramBindings() { return paramBindings; }
    public int timeoutMillis() { return timeoutMillis; }
    public int maxAttempts() { return maxAttempts; }
    public long backoffMillis() { return backoffMillis; }
    public String backoffStrategy() { return backoffStrategy; }
    public long maxBackoffMillis() { return maxBackoffMillis; }
    public Set<Class<? extends Throwable>> retryOn() { return retryOn; }
    public Set<Integer> ignoreStatus() { return ignoreStatus; }
    public Type returnType() { return returnType; }

    /** 是否允许重试。GET 是幂等的，POST 默认不重试。 */
    public boolean isRetryable() {
        return maxAttempts > 1;
    }

    /** 是否为 POST 请求（默认非幂等）。 */
    public boolean isPost() {
        return "POST".equalsIgnoreCase(httpMethod);
    }

    /** 检查此异常是否应触发重试。 */
    public boolean shouldRetryOn(Throwable ex) {
        if (retryOn.isEmpty()) return true; // 所有异常触发重试
        for (Class<? extends Throwable> cls : retryOn) {
            if (cls.isAssignableFrom(ex.getClass())) return true;
        }
        return false;
    }

    /** 检查此状态码是否应被忽略（不重试）。 */
    public boolean shouldIgnoreStatus(int status) {
        return ignoreStatus.contains(status);
    }
}
