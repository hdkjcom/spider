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
    void testAttribute() {
        SpiderRequest req = new SpiderRequest().attribute("traceId", "abc123");
        assertEquals("abc123", req.attributes().get("traceId"));
    }
}
