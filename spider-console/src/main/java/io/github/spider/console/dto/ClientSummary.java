package io.github.spider.console.dto;

/**
 * 单个客户端的汇总指标。
 */
public class ClientSummary {

    /** 所属服务名 */
    private String service;
    /** 客户端名称 */
    private String client;
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
    /** 累计总延迟（毫秒） */
    private long totalLatencyMs;
    /** 延迟 P50（毫秒） */
    private long p50;
    /** 延迟 P90（毫秒） */
    private long p90;
    /** 延迟 P99（毫秒） */
    private long p99;
    /** 成功率，如 "98.5" */
    private String successRate;
    /** 平均延迟，如 "12.3" */
    private String avgLatencyMs;

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
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
    public String getSuccessRate() { return successRate; }
    public void setSuccessRate(String successRate) { this.successRate = successRate; }
    public String getAvgLatencyMs() { return avgLatencyMs; }
    public void setAvgLatencyMs(String avgLatencyMs) { this.avgLatencyMs = avgLatencyMs; }
}
