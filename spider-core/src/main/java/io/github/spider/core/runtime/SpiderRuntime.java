package io.github.spider.core.runtime;

import io.github.spider.core.policy.SpiderCircuitBreaker;
import io.github.spider.core.runtime.dto.ClientStatsDto;
import io.github.spider.core.runtime.dto.ErrorEntryDto;
import io.github.spider.core.runtime.dto.FullReportDto;
import io.github.spider.core.runtime.dto.OutcomePointDto;
import io.github.spider.core.runtime.dto.RuntimeSummaryDto;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class SpiderRuntime {

    private static final SpiderRuntime INSTANCE = new SpiderRuntime();
    public static SpiderRuntime getInstance() { return INSTANCE; }

    private final Map<String, ClientStats> statsMap = new ConcurrentHashMap<>();
    private final Map<String, SpiderCircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
    private final ConcurrentLinkedQueue<ErrorEntry> recentErrors = new ConcurrentLinkedQueue<>();
    private static final int MAX_ERRORS = 50;
    private static final int MAX_OUTCOMES = 120;

    private SpiderRuntime() {}

    public void recordSuccess(String clientName) {
        ClientStats s = statsMap.computeIfAbsent(clientName, ClientStats::new);
        s.successCount.incrementAndGet();
        s.callCount.incrementAndGet();
        s.addOutcome(true);
    }

    public void recordFailure(String clientName) {
        ClientStats s = statsMap.computeIfAbsent(clientName, ClientStats::new);
        s.failureCount.incrementAndGet();
        s.callCount.incrementAndGet();
        s.addOutcome(false);
    }

    public void recordRetry(String clientName) {
        statsMap.computeIfAbsent(clientName, ClientStats::new).retryCount.incrementAndGet();
    }

    public void recordFallback(String clientName) {
        statsMap.computeIfAbsent(clientName, ClientStats::new).fallbackCount.incrementAndGet();
    }

    public void recordLatency(String clientName, long millis) {
        ClientStats s = statsMap.computeIfAbsent(clientName, ClientStats::new);
        s.totalLatencyMs.addAndGet(millis);
        s.callCount.incrementAndGet();
        s.addLatency(millis);
    }

    public void recordError(String clientName, String methodName, String message) {
        recordError(clientName, methodName, message, null);
    }

    /** 记录错误，包含异常类型用于分类。 */
    public void recordError(String clientName, String methodName, String message, String errorType) {
        ErrorEntry e = new ErrorEntry(clientName, methodName, message, errorType);
        recentErrors.add(e);
        while (recentErrors.size() > MAX_ERRORS) recentErrors.poll();
    }

    public void registerCircuitBreaker(String clientName, SpiderCircuitBreaker cb) {
        circuitBreakers.put(clientName, cb);
    }

    public long uptimeMillis() { return System.currentTimeMillis() - startTime.get(); }
    public Set<String> clientNames() { return statsMap.keySet(); }
    public ClientStats stats(String clientName) { return statsMap.get(clientName); }

    public Map<String, SpiderCircuitBreaker.State> circuitBreakerStates() {
        Map<String, SpiderCircuitBreaker.State> states = new LinkedHashMap<>();
        for (Map.Entry<String, SpiderCircuitBreaker> e : circuitBreakers.entrySet())
            states.put(e.getKey(), e.getValue().state());
        return states;
    }

    public RuntimeSummaryDto summary() {
        RuntimeSummaryDto s = new RuntimeSummaryDto();
        s.setUptimeSeconds(uptimeMillis() / 1000.0);
        s.setClientCount(statsMap.size());
        long calls = 0, ok = 0, fail = 0;
        for (ClientStats cs : statsMap.values()) { calls += cs.callCount.get(); ok += cs.successCount.get(); fail += cs.failureCount.get(); }
        s.setTotalCalls(calls);
        s.setTotalSuccess(ok);
        s.setTotalFailure(fail);
        s.setSuccessRate(calls > 0 ? String.format("%.1f%%", 100.0 * ok / calls) : "N/A");
        return s;
    }

    public List<ErrorEntryDto> recentErrors() {
        List<ErrorEntryDto> list = new ArrayList<>();
        for (ErrorEntry e : recentErrors) {
            ErrorEntryDto dto = new ErrorEntryDto();
            dto.setClient(e.clientName);
            dto.setMethod(e.methodName);
            dto.setMessage(e.message);
            dto.setTime(e.timestamp);
            if (e.errorType != null) dto.setErrorType(e.errorType);
            list.add(dto);
        }
        Collections.reverse(list);
        return list;
    }

    public FullReportDto fullReport() {
        FullReportDto r = new FullReportDto();
        r.setSummary(summary());
        r.setRecentErrors(recentErrors());
        Map<String, ClientStatsDto> clients = new LinkedHashMap<>();
        for (String name : clientNames()) {
            ClientStats cs = stats(name);
            if (cs == null) continue;
            ClientStatsDto cm = new ClientStatsDto();
            cm.setName(name);
            cm.setCalls(cs.callCount.get());
            cm.setSuccess(cs.successCount.get());
            cm.setFailure(cs.failureCount.get());
            cm.setRetries(cs.retryCount.get());
            cm.setFallbacks(cs.fallbackCount.get());
            cm.setAvgLatencyMs(cs.avgLatencyMs());
            cm.setP50(cs.latencyPercentile(50));
            cm.setP90(cs.latencyPercentile(90));
            cm.setP99(cs.latencyPercentile(99));
            long c = cs.callCount.get(), s = cs.successCount.get();
            cm.setSuccessRate(c > 0 ? 100.0 * s / c : 100.0);
            cm.setCurrentQps(cs.currentQps());
            clients.put(name, cm);
        }
        r.setClients(clients);
        Map<String, String> cbStringStates = new LinkedHashMap<>();
        for (Map.Entry<String, SpiderCircuitBreaker.State> e : circuitBreakerStates().entrySet()) {
            cbStringStates.put(e.getKey(), e.getValue().name());
        }
        r.setCircuitBreakers(cbStringStates);
        return r;
    }

    public static class ClientStats {
        public final String clientName;
        public final AtomicLong successCount = new AtomicLong(), failureCount = new AtomicLong(),
                retryCount = new AtomicLong(), fallbackCount = new AtomicLong(),
                callCount = new AtomicLong(), totalLatencyMs = new AtomicLong();
        private final ConcurrentLinkedQueue<OutcomePoint> outcomes = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
        private static final int MAX_LATENCIES = 200;

        ClientStats(String n) { this.clientName = n; }

        public double avgLatencyMs() { long c = callCount.get(); return c > 0 ? (double)totalLatencyMs.get()/c : 0; }

        public long latencyPercentile(int pct) {
            if (latencies.isEmpty()) return 0;
            Long[] sorted = latencies.toArray(new Long[0]);
            Arrays.sort(sorted);
            int idx = (int)Math.ceil(sorted.length * pct / 100.0) - 1;
            if (idx < 0) idx = 0; if (idx >= sorted.length) idx = sorted.length - 1;
            return sorted[idx];
        }

        void addOutcome(boolean success) {
            outcomes.add(new OutcomePoint(success));
            while (outcomes.size() > MAX_OUTCOMES) outcomes.poll();
        }

        void addLatency(long ms) {
            latencies.add(ms);
            while (latencies.size() > MAX_LATENCIES) latencies.poll();
        }

        public List<OutcomePointDto> outcomeHistory() {
            List<OutcomePointDto> list = new ArrayList<>();
            for (OutcomePoint p : outcomes) {
                OutcomePointDto dto = new OutcomePointDto();
                dto.setSuccess(p.success);
                dto.setTime(p.timestamp);
                list.add(dto);
            }
            return list;
        }

        public double currentQps() {
            long cutoff = System.currentTimeMillis() - 1000;
            long count = 0;
            for (OutcomePoint p : outcomes) if (p.timestamp >= cutoff) count++;
            return count;
        }

        static class OutcomePoint {
            final boolean success;
            final long timestamp = System.currentTimeMillis();
            OutcomePoint(boolean s) { this.success = s; }
        }
    }

    static class ErrorEntry {
        final String clientName, methodName, message, errorType;
        final long timestamp = System.currentTimeMillis();
        ErrorEntry(String c, String m, String msg) { this(c, m, msg, null); }
        ErrorEntry(String c, String m, String msg, String et) { clientName=c; methodName=m; message=msg; errorType=et; }
    }
}
