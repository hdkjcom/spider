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
    private int timeoutMillis = -1;   // -1 means use client/transport default
    private int maxAttempts = 1;      // default: 1 (no retry)
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

    /** Returns true if retry is allowed based on HTTP method. GET is idempotent, POST is not by default. */
    public boolean isRetryable() {
        return maxAttempts > 1;
    }

    /** Whether this is a POST request (non-idempotent by default). */
    public boolean isPost() {
        return "POST".equalsIgnoreCase(httpMethod);
    }

    /** Check if retry should be triggered for this exception. */
    public boolean shouldRetryOn(Throwable ex) {
        if (retryOn.isEmpty()) return true; // all exceptions trigger retry
        for (Class<? extends Throwable> cls : retryOn) {
            if (cls.isAssignableFrom(ex.getClass())) return true;
        }
        return false;
    }

    /** Check if this status code should be ignored (not retried). */
    public boolean shouldIgnoreStatus(int status) {
        return ignoreStatus.contains(status);
    }
}
