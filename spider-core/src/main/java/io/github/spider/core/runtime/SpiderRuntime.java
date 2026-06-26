package io.github.spider.core.runtime;

import io.github.spider.core.policy.SpiderCircuitBreaker;

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

    public Map<String, Object> summary() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("uptimeSeconds", uptimeMillis() / 1000.0);
        s.put("clientCount", statsMap.size());
        long calls = 0, ok = 0, fail = 0;
        for (ClientStats cs : statsMap.values()) { calls += cs.callCount.get(); ok += cs.successCount.get(); fail += cs.failureCount.get(); }
        s.put("totalCalls", calls); s.put("totalSuccess", ok); s.put("totalFailure", fail);
        s.put("successRate", calls > 0 ? String.format("%.1f%%", 100.0 * ok / calls) : "N/A");
        return s;
    }

    public List<Map<String, Object>> recentErrors() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ErrorEntry e : recentErrors) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("client", e.clientName); m.put("method", e.methodName);
            m.put("message", e.message); m.put("time", e.timestamp);
            if (e.errorType != null) m.put("errorType", e.errorType);
            list.add(m);
        }
        Collections.reverse(list);
        return list;
    }

    public Map<String, Object> fullReport() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("summary", summary());
        r.put("recentErrors", recentErrors());
        Map<String, Map<String, Object>> clients = new LinkedHashMap<>();
        for (String name : clientNames()) {
            ClientStats cs = stats(name);
            if (cs == null) continue;
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("name", name);
            cm.put("calls", cs.callCount.get());
            cm.put("success", cs.successCount.get());
            cm.put("failure", cs.failureCount.get());
            cm.put("retries", cs.retryCount.get());
            cm.put("fallbacks", cs.fallbackCount.get());
            cm.put("avgLatencyMs", String.format("%.2f", cs.avgLatencyMs()));
            cm.put("p50", cs.latencyPercentile(50));
            cm.put("p90", cs.latencyPercentile(90));
            cm.put("p99", cs.latencyPercentile(99));
            long c = cs.callCount.get(), s = cs.successCount.get();
            cm.put("successRate", c > 0 ? String.format("%.1f", 100.0 * s / c) : "100.0");
            cm.put("outcomes", cs.outcomeHistory());
            cm.put("currentQps", cs.currentQps());
            clients.put(name, cm);
        }
        r.put("clients", clients);
        r.put("circuitBreakers", circuitBreakerStates());
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

        public List<Map<String,Object>> outcomeHistory() {
            List<Map<String,Object>> list = new ArrayList<>();
            for (OutcomePoint p : outcomes) {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("success", p.success);
                m.put("time", p.timestamp);
                list.add(m);
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
