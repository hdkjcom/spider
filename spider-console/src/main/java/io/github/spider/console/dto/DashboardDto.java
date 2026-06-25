package io.github.spider.console.dto;

import java.util.*;

/**
 * 仪表盘完整数据。
 */
public class DashboardDto {

    /** 已上报的服务名列表 */
    private List<String> services = new ArrayList<>();
    /** 客户端汇总，key=服务名/客户端名 */
    private Map<String, ClientSummary> clients = new LinkedHashMap<>();
    /** 熔断器状态，key=客户端名，value=CLOSED/OPEN/HALF_OPEN */
    private Map<String, String> circuitBreakers = new LinkedHashMap<>();
    /** 数据点总数 */
    private int snapshotCount;
    /** 链路追踪是否已启用 */
    private boolean tracingEnabled;
    /** 数据生成时间 */
    private Date time = new Date();

    public List<String> getServices() { return services; }
    public void setServices(List<String> services) { this.services = services; }
    public Map<String, ClientSummary> getClients() { return clients; }
    public void setClients(Map<String, ClientSummary> clients) { this.clients = clients; }
    public Map<String, String> getCircuitBreakers() { return circuitBreakers; }
    public void setCircuitBreakers(Map<String, String> circuitBreakers) { this.circuitBreakers = circuitBreakers; }
    public int getSnapshotCount() { return snapshotCount; }
    public void setSnapshotCount(int snapshotCount) { this.snapshotCount = snapshotCount; }
    public boolean isTracingEnabled() { return tracingEnabled; }
    public void setTracingEnabled(boolean tracingEnabled) { this.tracingEnabled = tracingEnabled; }
    public Date getTime() { return time; }
    public void setTime(Date time) { this.time = time; }
}
