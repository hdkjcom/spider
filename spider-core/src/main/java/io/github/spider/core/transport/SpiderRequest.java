package io.github.spider.core.transport;

import java.util.*;

/**
 * Transport-agnostic request model, built by the framework from annotations and arguments.
 *
 * <p>Users rarely construct this directly. It is created by {@code RequestTemplate}
 * and passed through the invocation pipeline before reaching the transport.
 */
public class SpiderRequest {

    private String method;
    private String url;
    private String path;
    private Map<String, List<String>> queryParams = new HashMap<>();
    private Map<String, List<String>> headers = new HashMap<>();
    private byte[] body;
    private int timeoutMillis;
    private Map<String, Object> attributes = new HashMap<>();

    public SpiderRequest() {}

    public SpiderRequest method(String m) { this.method = m; return this; }
    public String method() { return method; }

    public SpiderRequest url(String u) { this.url = u; return this; }
    public String url() { return url; }

    public SpiderRequest path(String p) { this.path = p; return this; }
    public String path() { return path; }

    public SpiderRequest body(byte[] b) { this.body = b; return this; }
    public byte[] body() { return body; }

    public SpiderRequest timeoutMillis(int t) { this.timeoutMillis = t; return this; }
    public int timeoutMillis() { return timeoutMillis; }

    public SpiderRequest addQueryParam(String name, String value) {
        queryParams.computeIfAbsent(name, k -> new ArrayList<>()).add(value); return this;
    }
    public Map<String, List<String>> queryParams() { return queryParams; }

    public SpiderRequest addHeader(String name, String value) {
        headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value); return this;
    }
    public Map<String, List<String>> headers() { return headers; }

    /** Arbitrary key-value store for passing metadata through the pipeline (trace spans, etc.). */
    public SpiderRequest attribute(String key, Object value) { attributes.put(key, value); return this; }
    public Map<String, Object> attributes() { return attributes; }

    /** Assembles the full URL: {@code baseUrl + path}. */
    public String fullUrl() {
        StringBuilder sb = new StringBuilder();
        if (url != null) sb.append(url.replaceAll("/$", ""));
        if (path != null && !path.isEmpty()) {
            if (!path.startsWith("/")) sb.append('/');
            sb.append(path);
        }
        return sb.toString();
    }
}
