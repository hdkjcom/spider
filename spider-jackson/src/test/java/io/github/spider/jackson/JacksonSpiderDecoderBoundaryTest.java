package io.github.spider.jackson;

import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boundary tests for {@link JacksonSpiderDecoder} covering generic return
 * types, null/empty body handling, and the two String response shapes
 * (JSON-encoded vs. plain text).
 */
class JacksonSpiderDecoderBoundaryTest {

    private final JacksonSpiderDecoder decoder = new JacksonSpiderDecoder();

    /** Concrete {@code Type} literal for {@code List<TestUser>}. */
    private static final Type LIST_USER_TYPE = new ParameterizedType() {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{TestUser.class};
        }

        @Override
        public Type getRawType() {
            return List.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    };

    /** Concrete {@code Type} literal for {@code Map<String, Object>}. */
    private static final Type MAP_STRING_OBJECT_TYPE = new ParameterizedType() {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{String.class, Object.class};
        }

        @Override
        public Type getRawType() {
            return Map.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    };

    // ---- Generic return types ----

    @Test
    @SuppressWarnings("unchecked")
    void testDecodeGenericList() throws Exception {
        byte[] json = "[{\"name\":\"alice\",\"age\":30},{\"name\":\"bob\",\"age\":25}]"
                .getBytes();

        Object result = decoder.decode(json, LIST_USER_TYPE);

        assertNotNull(result);
        assertTrue(result instanceof List, "Decoded value must be a List");
        List<TestUser> users = (List<TestUser>) result;
        assertEquals(2, users.size());
        assertEquals("alice", users.get(0).getName());
        assertEquals(30, users.get(0).getAge());
        assertEquals("bob", users.get(1).getName());
        assertEquals(25, users.get(1).getAge());
    }

    @Test
    void testDecodeEmptyList() throws Exception {
        byte[] json = "[]".getBytes();

        Object result = decoder.decode(json, LIST_USER_TYPE);

        assertNotNull(result);
        assertTrue(result instanceof List);
        assertTrue(((List<?>) result).isEmpty(), "Empty JSON array must decode to an empty list");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDecodeGenericMap() throws Exception {
        byte[] json = "{\"name\":\"spider\",\"version\":8,\"nested\":{\"a\":1}}".getBytes();

        Object result = decoder.decode(json, MAP_STRING_OBJECT_TYPE);

        assertNotNull(result);
        assertTrue(result instanceof Map, "Decoded value must be a Map");
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("spider", map.get("name"));
        assertEquals(8, ((Number) map.get("version")).intValue());
        assertNotNull(map.get("nested"), "Nested object must be preserved");
        assertTrue(map.get("nested") instanceof Map, "Nested JSON object must decode to a Map");
    }

    // ---- null / empty body ----

    @Test
    void testDecodeNullBytesReturnsNull() throws Exception {
        Object result = decoder.decode(null, TestUser.class);
        assertNull(result, "Null body must decode to null");
    }

    @Test
    void testDecodeEmptyBytesReturnsNull() throws Exception {
        Object result = decoder.decode(new byte[0], TestUser.class);
        assertNull(result, "Empty body must decode to null");
    }

    @Test
    void testDecodeNullBytesWithGenericTypeReturnsNull() throws Exception {
        Object result = decoder.decode(null, LIST_USER_TYPE);
        assertNull(result, "Null body must decode to null even for generic types");
    }

    // ---- String response forms ----

    @Test
    void testDecodeJsonStringWithQuotes() throws Exception {
        // The canonical JSON form of a string: double-quoted.
        byte[] json = "\"hello\"".getBytes();

        Object result = decoder.decode(json, String.class);

        assertEquals("hello", result, "JSON-encoded string must unwrap to the bare value");
    }

    @Test
    void testDecodePlainTextStringWithoutQuotes() throws Exception {
        // A non-JSON plain-text body. The decoder falls back to a raw UTF-8
        // string when Jackson cannot parse it as a JSON string.
        byte[] plain = "hello".getBytes();

        Object result = decoder.decode(plain, String.class);

        assertEquals("hello", result,
                "Plain-text (non-JSON) String body must fall back to the raw value");
    }

    @Test
    void testDecodeEmptyJsonStringWithQuotes() throws Exception {
        byte[] json = "\"\"".getBytes();

        Object result = decoder.decode(json, String.class);

        assertNotNull(result);
        assertEquals("", result, "JSON empty string must decode to an empty String, not null");
    }

    /** Shared test object reused by both decoder test classes. */
    static class TestUser {
        private String name;
        private int age;

        public TestUser() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
}
