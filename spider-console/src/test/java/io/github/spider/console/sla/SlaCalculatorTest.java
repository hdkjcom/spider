package io.github.spider.console.sla;

import io.github.spider.console.dto.ClientSummary;
import io.github.spider.console.dto.SlaDto;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SlaCalculator 纯逻辑单测，不启动 Spring 上下文。
 */
class SlaCalculatorTest {

    private static ClientSummary cs(long calls, long success, long failure, long p99) {
        ClientSummary c = new ClientSummary();
        c.setCalls(calls);
        c.setSuccess(success);
        c.setFailure(failure);
        c.setP99(p99);
        return c;
    }

    @Test
    void noDataWhenNoCalls() {
        SlaDto sla = SlaCalculator.compute(Collections.<ClientSummary>emptyList(), 99.9, 500, 1.0);
        assertEquals(SlaDto.NO_DATA, sla.getAvailability().getStatus());
        assertEquals(SlaDto.NO_DATA, sla.getLatencyP99().getStatus());
        assertEquals(SlaDto.NO_DATA, sla.getErrorRate().getStatus());
    }

    @Test
    void okWhenAllTargetsMet() {
        // 1000 calls, 999 success, 1 failure → avail 99.9%, err 0.1%, p99 300ms
        SlaDto sla = SlaCalculator.compute(Arrays.asList(cs(1000, 999, 1, 300)), 99.9, 500, 1.0);
        assertEquals(SlaDto.OK, sla.getAvailability().getStatus());
        assertEquals(SlaDto.OK, sla.getLatencyP99().getStatus());
        assertEquals(SlaDto.OK, sla.getErrorRate().getStatus());
        assertEquals(99.9, sla.getAvailability().getValue(), 0.001);
    }

    @Test
    void breachWhenLatencyExceedsTarget() {
        SlaDto sla = SlaCalculator.compute(Arrays.asList(cs(100, 100, 0, 600)), 99.9, 500, 1.0);
        assertEquals(SlaDto.BREACH, sla.getLatencyP99().getStatus());
        assertEquals(SlaDto.OK, sla.getAvailability().getStatus());
    }

    @Test
    void breachWhenErrorRateExceedsTarget() {
        // 100 calls, 98 success, 2 failure → err 2.0% > 1.0%
        SlaDto sla = SlaCalculator.compute(Arrays.asList(cs(100, 98, 2, 300)), 99.9, 500, 1.0);
        assertEquals(SlaDto.BREACH, sla.getErrorRate().getStatus());
    }

    @Test
    void aggregatesAcrossClientsAndTakesMaxP99() {
        SlaDto sla = SlaCalculator.compute(Arrays.asList(cs(50, 50, 0, 200), cs(50, 50, 0, 400)), 99.9, 500, 1.0);
        assertEquals(400.0, sla.getLatencyP99().getValue(), 0.001);
        assertEquals(SlaDto.OK, sla.getLatencyP99().getStatus());
    }
}
