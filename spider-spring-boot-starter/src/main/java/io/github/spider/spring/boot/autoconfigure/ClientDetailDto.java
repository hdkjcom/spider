package io.github.spider.spring.boot.autoconfigure;

/**
 * DTO returned by the {@code /actuator/spider/clients/{name}} endpoint.
 */
public class ClientDetailDto {

    private String name;
    private String error;
    private long calls;
    private long success;
    private long failure;
    private long retries;
    private long fallbacks;
    private String avgLatencyMs;
    private long p50;
    private long p90;
    private long p99;
    private String successRate;
    private double currentQps;

    public ClientDetailDto() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public long getCalls() {
        return calls;
    }

    public void setCalls(long calls) {
        this.calls = calls;
    }

    public long getSuccess() {
        return success;
    }

    public void setSuccess(long success) {
        this.success = success;
    }

    public long getFailure() {
        return failure;
    }

    public void setFailure(long failure) {
        this.failure = failure;
    }

    public long getRetries() {
        return retries;
    }

    public void setRetries(long retries) {
        this.retries = retries;
    }

    public long getFallbacks() {
        return fallbacks;
    }

    public void setFallbacks(long fallbacks) {
        this.fallbacks = fallbacks;
    }

    public String getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(String avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    public long getP50() {
        return p50;
    }

    public void setP50(long p50) {
        this.p50 = p50;
    }

    public long getP90() {
        return p90;
    }

    public void setP90(long p90) {
        this.p90 = p90;
    }

    public long getP99() {
        return p99;
    }

    public void setP99(long p99) {
        this.p99 = p99;
    }

    public String getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(String successRate) {
        this.successRate = successRate;
    }

    public double getCurrentQps() {
        return currentQps;
    }

    public void setCurrentQps(double currentQps) {
        this.currentQps = currentQps;
    }
}
