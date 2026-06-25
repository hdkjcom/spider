package io.github.spider.core.client;

import io.github.spider.core.transport.SpiderResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpiderResponseContextTest {

    @Test
    void testSetAndGet() {
        SpiderResponse resp = new SpiderResponse().statusCode(201)
                .addHeader("X-Id", "abc");
        SpiderResponseContext.set(resp);

        SpiderResponse got = SpiderResponseContext.lastResponse();
        assertNotNull(got);
        assertEquals(201, got.statusCode());
        assertEquals("abc", got.headers().get("X-Id").get(0));
    }

    @Test
    void testClear() {
        SpiderResponseContext.set(new SpiderResponse().statusCode(200));
        SpiderResponseContext.clear();
        assertNull(SpiderResponseContext.lastResponse());
    }

    @Test
    void testInitiallyNull() {
        SpiderResponseContext.clear();
        assertNull(SpiderResponseContext.lastResponse());
    }
}
