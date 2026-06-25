package io.github.spider.console.dto;

/**
 * 链路追踪状态。
 */
public class TracingDto {
    /** 是否已启用 */
    private boolean enabled;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
