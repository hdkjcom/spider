package io.github.spider.jackson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JacksonSpiderEncoderTest {

    private final JacksonSpiderEncoder encoder = new JacksonSpiderEncoder();

    @Test
    void testEncodeSimpleObject() throws Exception {
        TestUser user = new TestUser("zhangsan", 25);
        byte[] bytes = encoder.encode(user);

        String json = new String(bytes);
        assertTrue(json.contains("\"name\":\"zhangsan\""));
        assertTrue(json.contains("\"age\":25"));
    }

    @Test
    void testEncodeNull() throws Exception {
        byte[] bytes = encoder.encode(null);
        assertEquals(0, bytes.length);
    }

    @Test
    void testEncodeString() throws Exception {
        byte[] bytes = encoder.encode("hello");
        assertEquals("\"hello\"", new String(bytes));
    }

    static class TestUser {
        private String name;
        private int age;

        public TestUser() {}

        public TestUser(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
}
