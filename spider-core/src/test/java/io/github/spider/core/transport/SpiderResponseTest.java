package io.github.spider.core.transport;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpiderResponseTest {

    @Test
    void testSuccessfulStatusCodes() {
        assertTrue(new SpiderResponse().statusCode(200).isSuccessful());
        assertTrue(new SpiderResponse().statusCode(201).isSuccessful());
        assertTrue(new SpiderResponse().statusCode(299).isSuccessful());
    }

    @Test
    void testNonSuccessfulStatusCodes() {
        assertFalse(new SpiderResponse().statusCode(300).isSuccessful());
        assertFalse(new SpiderResponse().statusCode(400).isSuccessful());
        assertFalse(new SpiderResponse().statusCode(500).isSuccessful());
    }

    @Test
    void testElapsedMillis() {
        SpiderResponse resp = new SpiderResponse().elapsedMillis(150);
        assertEquals(150, resp.elapsedMillis());
    }
}
