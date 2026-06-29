package io.github.spider.core.runtime.dto;

public class RuntimeSummaryDto {

    private double uptimeSeconds;
    private int clientCount;
    private long totalCalls;
    private long totalSuccess;
    private long totalFailure;
    private String successRate;

    public double getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(double uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public int getClientCount() {
        return clientCount;
    }

    public void setClientCount(int clientCount) {
        this.clientCount = clientCount;
    }

    public long getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(long totalCalls) {
        this.totalCalls = totalCalls;
    }

    public long getTotalSuccess() {
        return totalSuccess;
    }

    public void setTotalSuccess(long totalSuccess) {
        this.totalSuccess = totalSuccess;
    }

    public long getTotalFailure() {
        return totalFailure;
    }

    public void setTotalFailure(long totalFailure) {
        this.totalFailure = totalFailure;
    }

    public String getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(String successRate) {
        this.successRate = successRate;
    }
}
