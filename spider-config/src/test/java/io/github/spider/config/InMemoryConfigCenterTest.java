package io.github.spider.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryConfigCenterTest {

    @Test
    void testGetAndPut() {
        InMemoryConfigCenter center = new InMemoryConfigCenter();
        center.put("spider.default-timeout", "3000");
        assertEquals("3000", center.get("spider.default-timeout", "5000"));
        assertEquals(3000, center.getInt("spider.default-timeout", 5000));
    }

    @Test
    void testDefaultValue() {
        InMemoryConfigCenter center = new InMemoryConfigCenter();
        assertEquals("default", center.get("nonexistent", "default"));
        assertEquals(5000, center.getInt("nonexistent", 5000));
    }

    @Test
    void testListenerNotification() {
        InMemoryConfigCenter center = new InMemoryConfigCenter();
        final boolean[] notified = {false};
        center.addListener(changed -> {
            if (changed.containsKey("timeout")) notified[0] = true;
        });
        center.put("timeout", "1000");
        assertTrue(notified[0]);
    }

    @Test
    void testNoopDoesNotThrow() {
        SpiderConfigCenter noop = SpiderConfigCenter.NOOP;
        assertEquals("default", noop.get("key", "default"));
        assertEquals(5000, noop.getInt("key", 5000));
        noop.addListener(changed -> {});
        noop.watch("key");
    }
}
