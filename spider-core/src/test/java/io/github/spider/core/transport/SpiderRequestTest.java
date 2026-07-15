package io.github.spider.core.transport;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpiderRequestTest {

    @Test
    void testFullUrlConcatenation() {
        SpiderRequest req = new SpiderRequest()
                .url("http://localhost:8081")
                .path("/users/1");

        assertEquals("http://localhost:8081/users/1", req.fullUrl());
    }

    @Test
    void testFullUrlWithoutTrailingSlash() {
        SpiderRequest req = new SpiderRequest()
                .url("http://localhost:8081/")
                .path("/users/1");

        assertEquals("http://localhost:8081/users/1", req.fullUrl());
    }

    @Test
    void testPathWithoutLeadingSlash() {
        SpiderRequest req = new SpiderRequest()
                .url("http://localhost:8081")
                .path("users/1");

        assertEquals("http://localhost:8081/users/1", req.fullUrl());
    }

    @Test
    void testMethodSetter() {
        SpiderRequest req = new SpiderRequest().method("GET");
        assertEquals("GET", req.method());
    }

    @Test
    void testAddQueryParam() {
        SpiderRequest req = new SpiderRequest()
                .addQueryParam("key", "value1")
                .addQueryParam("key", "value2");

        assertEquals(2, req.queryParams().get("key").size());
        assertEquals("value1", req.queryParams().get("key").get(0));
        assertEquals("value2", req.queryParams().get("key").get(1));
    }

    @Test
    void testAddHeader() {
        SpiderRequest req = new SpiderRequest()
                .addHeader("Authorization", "Bearer token");

        assertEquals("Bearer token", req.headers().get("Authorization").get(0));
    }

    @Test
    void testTimeout() {
        SpiderRequest req = new SpiderRequest().timeoutMillis(800);
        assertEquals(800, req.timeoutMillis());
    }

    @Test
    void testContentType() {
        SpiderRequest req = new SpiderRequest().contentType("application/xml");
        assertEquals("application/xml", req.contentType());
    }

    @Test
    void testAttribute() {
        SpiderRequest req = new SpiderRequest().attribute("traceId", "abc123");
        assertEquals("abc123", req.attributes().get("traceId"));
    }

    @Test
    void fullUrlAppendsQueryParams() {
        SpiderRequest req = new SpiderRequest()
                .url("http://localhost:8081")
                .path("/sns/jscode2session")
                .addQueryParam("appid", "wx123")
                .addQueryParam("grant_type", "code");
        String url = req.fullUrl();
        assertTrue(url.startsWith("http://localhost:8081/sns/jscode2session?"), url);
        assertTrue(url.contains("appid=wx123"), url);
        assertTrue(url.contains("grant_type=code"), url);
    }

    @Test
    void fullUrlEncodesSpecialCharsInQuery() {
        // js_code 含空格 / + / 斜杠（base64 样）应被 percent-encode，避免破坏 URL
        SpiderRequest req = new SpiderRequest()
                .url("http://localhost:8081")
                .path("/sns/jscode2session")
                .addQueryParam("js_code", "ab c+ d/");
        String url = req.fullUrl();
        assertTrue(url.contains("js_code=ab+c%2B+d%2F"), url);
    }
}
