package io.github.spider.console.sla;

import io.github.spider.console.dto.ClientSummary;
import io.github.spider.console.dto.SlaDto;

import java.util.Collection;

/**
 * SLA 达标快览计算：聚合客户端统计，对照 SLO 目标判定 OK / BREACH / NO_DATA。
 *
 * <p>纯逻辑，不依赖 Spring，便于单元测试（避开 web 上下文启动）。
 */
public final class SlaCalculator {

    private SlaCalculator() {}

    public static SlaDto compute(Collection<ClientSummary> clients,
                                 double sloAvailability, double sloLatencyP99, double sloErrorRate) {
        long totalCalls = 0, totalSuccess = 0, totalFailure = 0, maxP99 = 0;
        for (ClientSummary cs : clients) {
            totalCalls += cs.getCalls();
            totalSuccess += cs.getSuccess();
            totalFailure += cs.getFailure();
            if (cs.getP99() > maxP99) maxP99 = cs.getP99();
        }
        SlaDto sla = new SlaDto();
        if (totalCalls == 0) {
            sla.setAvailability(new SlaDto.Indicator(0, sloAvailability, SlaDto.NO_DATA, "%"));
            sla.setLatencyP99(new SlaDto.Indicator(0, sloLatencyP99, SlaDto.NO_DATA, "ms"));
            sla.setErrorRate(new SlaDto.Indicator(0, sloErrorRate, SlaDto.NO_DATA, "%"));
            return sla;
        }
        double avail = 100.0 * totalSuccess / totalCalls;
        double err = 100.0 * totalFailure / totalCalls;
        sla.setAvailability(new SlaDto.Indicator(round2(avail), sloAvailability,
                avail >= sloAvailability ? SlaDto.OK : SlaDto.BREACH, "%"));
        sla.setLatencyP99(new SlaDto.Indicator(maxP99, sloLatencyP99,
                maxP99 <= sloLatencyP99 ? SlaDto.OK : SlaDto.BREACH, "ms"));
        sla.setErrorRate(new SlaDto.Indicator(round2(err), sloErrorRate,
                err <= sloErrorRate ? SlaDto.OK : SlaDto.BREACH, "%"));
        return sla;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
