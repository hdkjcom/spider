package io.github.spider.core.metadata;

import io.github.spider.core.annotation.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class DefaultMethodMetadataParserTest {

    private final DefaultMethodMetadataParser parser = new DefaultMethodMetadataParser();

    // --- Test interface ---

    @SpiderClient(name = "test", url = "http://localhost:8080")
    interface TestClient {

        @SpiderGet("/users/{id}")
        @Timeout(500)
        @Retry(maxAttempts = 3, backoffMillis = 200)
        String getUser(@Path("id") Long id);

        @SpiderPost("/users")
        String createUser(@Body CreateRequest body);

        @SpiderGet("/search")
        String search(@Query("q") String keyword, @Header("Authorization") String token);

        @SpiderPut("/users/{id}")
        String updateUser(@Path("id") Long id, @Body UpdateRequest body);

        @SpiderDelete("/users/{id}")
        void deleteUser(@Path("id") Long id);

        void nonAnnotated(); // should be skipped
    }

    static class CreateRequest {
        public String name;
    }

    static class UpdateRequest {
        public String name;
        public int age;
    }

    // --- Tests ---

    @Test
    void testParseGetMethod() throws Exception {
        Method method = TestClient.class.getMethod("getUser", Long.class);
        MethodMetadata meta = parser.parse(method);

        assertNotNull(meta);
        assertEquals("GET", meta.httpMethod());
        assertEquals("/users/{id}", meta.pathTemplate());
        assertEquals(500, meta.timeoutMillis());
        assertEquals(3, meta.maxAttempts());
        assertEquals(200, meta.backoffMillis());
        assertEquals(String.class, meta.returnType());
        assertTrue(meta.isRetryable());

        assertEquals(1, meta.paramBindings().size());
        ParamBinding binding = meta.paramBindings().get(0);
        assertEquals(ParamBinding.Kind.PATH, binding.kind());
        assertEquals("id", binding.name());
        assertEquals(0, binding.index());
    }

    @Test
    void testParsePostMethod() throws Exception {
        Method method = TestClient.class.getMethod("createUser", CreateRequest.class);
        MethodMetadata meta = parser.parse(method);

        assertNotNull(meta);
        assertEquals("POST", meta.httpMethod());
        assertEquals("/users", meta.pathTemplate());
        assertEquals(String.class, meta.returnType());

        assertEquals(1, meta.paramBindings().size());
        ParamBinding binding = meta.paramBindings().get(0);
        assertEquals(ParamBinding.Kind.BODY, binding.kind());
        assertEquals(0, binding.index());
    }

    @Test
    void testParseQueryAndHeader() throws Exception {
        Method method = TestClient.class.getMethod("search", String.class, String.class);
        MethodMetadata meta = parser.parse(method);

        assertNotNull(meta);
        assertEquals("GET", meta.httpMethod());
        assertEquals("/search", meta.pathTemplate());

        assertEquals(2, meta.paramBindings().size());

        ParamBinding queryBinding = meta.paramBindings().get(0);
        assertEquals(ParamBinding.Kind.QUERY, queryBinding.kind());
        assertEquals("q", queryBinding.name());
        assertEquals(0, queryBinding.index());

        ParamBinding headerBinding = meta.paramBindings().get(1);
        assertEquals(ParamBinding.Kind.HEADER, headerBinding.kind());
        assertEquals("Authorization", headerBinding.name());
        assertEquals(1, headerBinding.index());
    }

    @Test
    void testNonAnnotatedMethod() throws Exception {
        Method method = TestClient.class.getMethod("nonAnnotated");
        MethodMetadata meta = parser.parse(method);

        assertNull(meta);
    }

    @Test
    void testDefaultRetrySettings() throws Exception {
        Method method = TestClient.class.getMethod("createUser", CreateRequest.class);
        MethodMetadata meta = parser.parse(method);

        // POST: default is no retry (maxAttempts = 1)
        assertEquals(1, meta.maxAttempts());
        assertFalse(meta.isRetryable());
    }
}
