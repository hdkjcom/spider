package io.github.spider.core.metadata;

import io.github.spider.core.codec.SpiderEncoder;
import io.github.spider.core.transport.SpiderRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestTemplateTest {

    private final SpiderEncoder noopEncoder = obj -> obj.toString().getBytes();
    private final RequestTemplate template = new RequestTemplate(noopEncoder);

    @Test
    void testBuildGetRequest() throws Exception {
        MethodMetadata meta = new MethodMetadata()
                .httpMethod("GET")
                .pathTemplate("/users/{id}")
                .timeoutMillis(800);
        meta.addParamBinding(new ParamBinding(ParamBinding.Kind.PATH, "id", 0));

        SpiderRequest request = template.build(meta, new Object[]{42L}, "http://localhost:8081");

        assertEquals("GET", request.method());
        assertEquals("http://localhost:8081", request.url());
        assertEquals("/users/42", request.path());
        assertEquals("http://localhost:8081/users/42", request.fullUrl());
        assertEquals(800, request.timeoutMillis());
    }

    @Test
    void testBuildWithQueryParams() throws Exception {
        MethodMetadata meta = new MethodMetadata()
                .httpMethod("GET")
                .pathTemplate("/search");
        meta.addParamBinding(new ParamBinding(ParamBinding.Kind.QUERY, "q", 0));
        meta.addParamBinding(new ParamBinding(ParamBinding.Kind.QUERY, "page", 1));

        SpiderRequest request = template.build(meta, new Object[]{"hello", "1"}, "http://localhost:8081");

        assertEquals("/search", request.path());
        assertEquals("hello", request.queryParams().get("q").get(0));
        assertEquals("1", request.queryParams().get("page").get(0));
    }

    @Test
    void testBuildWithHeader() throws Exception {
        MethodMetadata meta = new MethodMetadata()
                .httpMethod("GET")
                .pathTemplate("/users");
        meta.addParamBinding(new ParamBinding(ParamBinding.Kind.HEADER, "Authorization", 0));

        SpiderRequest request = template.build(meta, new Object[]{"Bearer token123"}, "http://localhost:8081");

        assertEquals("Bearer token123", request.headers().get("Authorization").get(0));
    }

    @Test
    void testBuildPostWithBody() throws Exception {
        MethodMetadata meta = new MethodMetadata()
                .httpMethod("POST")
                .pathTemplate("/users");
        meta.addParamBinding(new ParamBinding(ParamBinding.Kind.BODY, null, 0));

        String bodyContent = "{\"name\":\"test\"}";
        SpiderRequest request = template.build(meta, new Object[]{bodyContent}, "http://localhost:8081");

        assertEquals("POST", request.method());
        assertNotNull(request.body());
        assertArrayEquals(bodyContent.getBytes(), request.body());
    }

    @Test
    void testBuildWithMultiplePathVariables() throws Exception {
        MethodMetadata meta = new MethodMetadata()
                .httpMethod("GET")
                .pathTemplate("/orgs/{orgId}/users/{userId}");
        meta.addParamBinding(new ParamBinding(ParamBinding.Kind.PATH, "orgId", 0));
        meta.addParamBinding(new ParamBinding(ParamBinding.Kind.PATH, "userId", 1));

        SpiderRequest request = template.build(meta, new Object[]{10L, 20L}, "http://localhost:8081");

        assertEquals("/orgs/10/users/20", request.path());
    }

    @Test
    void testBuildNullArgsHandling() throws Exception {
        MethodMetadata meta = new MethodMetadata()
                .httpMethod("GET")
                .pathTemplate("/users/{id}");
        meta.addParamBinding(new ParamBinding(ParamBinding.Kind.PATH, "id", 0));

        SpiderRequest request = template.build(meta, null, "http://localhost:8081");

        // Path variable NOT replaced when args is null
        assertEquals("/users/{id}", request.path());
    }
}
