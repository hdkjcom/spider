package io.github.spider.jackson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JacksonSpiderDecoderTest {

    private final JacksonSpiderDecoder decoder = new JacksonSpiderDecoder();

    @Test
    void testDecodeSimpleObject() throws Exception {
        byte[] json = "{\"name\":\"lisi\",\"age\":30}".getBytes();
        TestUser user = (TestUser) decoder.decode(json, TestUser.class);

        assertNotNull(user);
        assertEquals("lisi", user.getName());
        assertEquals(30, user.getAge());
    }

    @Test
    void testDecodeNullBytes() throws Exception {
        Object result = decoder.decode(null, TestUser.class);
        assertNull(result);
    }

    @Test
    void testDecodeEmptyBytes() throws Exception {
        Object result = decoder.decode(new byte[0], TestUser.class);
        assertNull(result);
    }

    @Test
    void testDecodeString() throws Exception {
        byte[] json = "\"hello world\"".getBytes();
        String result = (String) decoder.decode(json, String.class);
        assertEquals("hello world", result);
    }

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
