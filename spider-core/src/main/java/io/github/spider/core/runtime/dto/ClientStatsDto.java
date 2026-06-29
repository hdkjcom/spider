package io.github.spider.core.runtime.dto;

/**
 * Per-client statistics DTO.
 */
public class ClientStatsDto {

    private String name;
    private long calls;
    private long success;
    private long failure;
    private long retries;
    private long fallbacks;
    private double avgLatencyMs;
    private long p50;
    private long p90;
    private long p99;
    private double successRate;
    private double currentQps;

    public ClientStatsDto() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(double avgLatencyMs) {
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

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public double getCurrentQps() {
        return currentQps;
    }

    public void setCurrentQps(double currentQps) {
        this.currentQps = currentQps;
    }
}
