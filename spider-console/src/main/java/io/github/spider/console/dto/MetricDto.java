package io.github.spider.console.dto;

/**
 * 客户端单次上报的指标数据。
 */
public class MetricDto {

    /** 客户端名称，如 wechat-api */
    private String client;
    /** 方法名 */
    private String method;
    /** 累计调用次数 */
    private long calls;
    /** 累计成功次数 */
    private long success;
    /** 累计失败次数 */
    private long failure;
    /** 累计重试次数 */
    private long retries;
    /** 累计降级次数 */
    private long fallbacks;
    /** 累计总延迟（毫秒），用于计算平均延迟 */
    private long totalLatencyMs;
    /** 延迟 P50（毫秒） */
    private long p50;
    /** 延迟 P90（毫秒） */
    private long p90;
    /** 延迟 P99（毫秒） */
    private long p99;

    public String getClient() { return client; }
    public void setClient(String client) { this.client = client; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public long getCalls() { return calls; }
    public void setCalls(long calls) { this.calls = calls; }
    public long getSuccess() { return success; }
    public void setSuccess(long success) { this.success = success; }
    public long getFailure() { return failure; }
    public void setFailure(long failure) { this.failure = failure; }
    public long getRetries() { return retries; }
    public void setRetries(long retries) { this.retries = retries; }
    public long getFallbacks() { return fallbacks; }
    public void setFallbacks(long fallbacks) { this.fallbacks = fallbacks; }
    public long getTotalLatencyMs() { return totalLatencyMs; }
    public void setTotalLatencyMs(long totalLatencyMs) { this.totalLatencyMs = totalLatencyMs; }
    public long getP50() { return p50; }
    public void setP50(long p50) { this.p50 = p50; }
    public long getP90() { return p90; }
    public void setP90(long p90) { this.p90 = p90; }
    public long getP99() { return p99; }
    public void setP99(long p99) { this.p99 = p99; }
}
