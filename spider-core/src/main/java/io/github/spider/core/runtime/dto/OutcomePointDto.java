package io.github.spider.core.runtime.dto;

/**
 * A single outcome data point for timeline charts.
 */
public class OutcomePointDto {

    private boolean success;
    private long time;

    public OutcomePointDto() {
    }

    public OutcomePointDto(boolean success, long time) {
        this.success = success;
        this.time = time;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
