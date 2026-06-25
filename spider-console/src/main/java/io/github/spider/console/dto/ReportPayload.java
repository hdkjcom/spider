package io.github.spider.console.dto;

import java.util.List;
import java.util.Map;

/**
 * 控制台上报请求体。
 */
public class ReportPayload {

    /** 上报的服务名，如 ppmt-swap */
    private String service;
    /** 所有客户端的指标快照列表 */
    private List<MetricDto> metrics;
    /** 熔断器状态快照，key=客户端名，value=CLOSED/OPEN/HALF_OPEN */
    private Map<String, String> circuitBreakers;
    /** 链路追踪信息 */
    private TracingDto tracing;

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public List<MetricDto> getMetrics() { return metrics; }
    public void setMetrics(List<MetricDto> metrics) { this.metrics = metrics; }
    public Map<String, String> getCircuitBreakers() { return circuitBreakers; }
    public void setCircuitBreakers(Map<String, String> circuitBreakers) { this.circuitBreakers = circuitBreakers; }
    public TracingDto getTracing() { return tracing; }
    public void setTracing(TracingDto tracing) { this.tracing = tracing; }
}
