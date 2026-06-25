package io.github.spider.console.dto;

/**
 * 上报接口的响应结果。
 */
public class ReportResult {

    /** 处理状态：ok 表示成功 */
    private String status = "ok";
    /** 成功保存的数据条数 */
    private int saved;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getSaved() { return saved; }
    public void setSaved(int saved) { this.saved = saved; }
}
