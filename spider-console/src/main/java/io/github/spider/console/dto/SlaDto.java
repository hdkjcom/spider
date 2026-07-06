package io.github.spider.console.dto;

/**
 * SLA 达标快览：基于自进程启动以来的快照统计，对照 SLO 目标判定 OK / BREACH / NO_DATA。
 *
 * <p>非严格 SLA——数据是进程内累积（重启清零）。长期 / 多实例 SLA 应通过 Prometheus 抓取
 * {@code spider.client.*} 指标，在 Grafana 配置 SLO 面板。
 */
public class SlaDto {

    public static final String OK = "OK";
    public static final String BREACH = "BREACH";
    public static final String NO_DATA = "NO_DATA";

    private Indicator availability;
    private Indicator latencyP99;
    private Indicator errorRate;

    public Indicator getAvailability() { return availability; }
    public void setAvailability(Indicator availability) { this.availability = availability; }
    public Indicator getLatencyP99() { return latencyP99; }
    public void setLatencyP99(Indicator latencyP99) { this.latencyP99 = latencyP99; }
    public Indicator getErrorRate() { return errorRate; }
    public void setErrorRate(Indicator errorRate) { this.errorRate = errorRate; }

    /** 单个 SLA 指标：实际值、目标、达标状态、单位。 */
    public static class Indicator {
        private double value;
        private double target;
        private String status;
        private String unit;

        public Indicator() {}

        public Indicator(double value, double target, String status, String unit) {
            this.value = value;
            this.target = target;
            this.status = status;
            this.unit = unit;
        }

        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
        public double getTarget() { return target; }
        public void setTarget(double target) { this.target = target; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }
}
