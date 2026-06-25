package io.github.spider.core.transport;

import java.util.*;

/**
 * Transport-agnostic response model returned by {@link SpiderTransport#execute}.
 */
public class SpiderResponse {

    private int statusCode;
    private Map<String, List<String>> headers;
    private byte[] bodyBytes;
    private long elapsedMillis;

    public SpiderResponse() {}

    public SpiderResponse(int statusCode, Map<String, List<String>> headers, byte[] bodyBytes, long elapsedMillis) {
        this.statusCode = statusCode; this.headers = headers;
        this.bodyBytes = bodyBytes; this.elapsedMillis = elapsedMillis;
    }

    public int statusCode() { return statusCode; }
    public SpiderResponse statusCode(int s) { this.statusCode = s; return this; }

    public Map<String, List<String>> headers() { return headers; }
    public SpiderResponse headers(Map<String, List<String>> h) { this.headers = h; return this; }

    public SpiderResponse addHeader(String name, String value) {
        if (headers == null) headers = new LinkedHashMap<>();
        headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }

    public byte[] bodyBytes() { return bodyBytes; }
    public SpiderResponse bodyBytes(byte[] b) { this.bodyBytes = b; return this; }

    public long elapsedMillis() { return elapsedMillis; }
    public SpiderResponse elapsedMillis(long e) { this.elapsedMillis = e; return this; }

    /** True for status codes 200-299. */
    public boolean isSuccessful() { return statusCode >= 200 && statusCode < 300; }
}
