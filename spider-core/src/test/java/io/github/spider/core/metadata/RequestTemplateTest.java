package io.github.spider.core.metadata;

import io.github.spider.core.codec.SpiderEncoder;
import io.github.spider.core.transport.SpiderRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestTemplateTest {

    private final SpiderEncoder noopEncoder = obj -> obj.toString().getBytes();
    private final RequestTemplate template = new RequestTemplate(noopEncoder);

    /** 显式声明非 JSON 媒体类型的 encoder（匿名类，用于验证 encoder.contentType() 被采纳）。 */
    private final SpiderEncoder xmlEncoder = new SpiderEncoder() {
        @Override
        public byte[] encode(Object object) throws Exception {
            return object.toString().getBytes();
        }

        @Override
        public String contentType() {
            return "application/xml";
        }
    };

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
        // 无显式声明时，content-type 来自 encoder 默认（lambda 继承 default 方法）
        assertEquals(SpiderEncoder.DEFAULT_CONTENT_TYPE, request.contentType());
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
    void testBuildNullArgsHandling() {
        MethodMetadata meta = new MethodMetadata()
                .httpMethod("GET")
                .pathTemplate("/users/{id}");
        meta.addParamBinding(new ParamBinding(ParamBinding.Kind.PATH, "id", 0));

        // PATH 参数为 null 时应及早抛异常，避免 {id} 残留进 URL 导致下游 404
        assertThrows(io.github.spider.core.exception.SpiderConfigurationException.class,
                () -> template.build(meta, null, "http://localhost:8081"));
    }

    @Test
    void testBuildPostWithBodyContentTypeOverride() throws Exception {
        MethodMetadata meta = new MethodMetadata()
                .httpMethod("POST")
                .pathTemplate("/xml");
        // @Body.contentType 通过 4 参 ParamBinding 传入
        meta.addParamBinding(new ParamBinding(ParamBinding.Kind.BODY, null, 0, "application/xml"));

        SpiderRequest request = template.build(meta, new Object[]{"<x/>"}, "http://localhost:8081");

        assertEquals("application/xml", request.contentType());
    }

    @Test
    void testBuildPostWithBodyEmptyContentTypeFallsToEncoder() throws Exception {
        MethodMetadata meta = new MethodMetadata()
                .httpMethod("POST")
                .pathTemplate("/xml");
        // 空串视为未声明
        meta.addParamBinding(new ParamBinding(ParamBinding.Kind.BODY, null, 0, ""));

        SpiderRequest request = template.build(meta, new Object[]{"<x/>"}, "http://localhost:8081");

        assertEquals(SpiderEncoder.DEFAULT_CONTENT_TYPE, request.contentType());
    }

    @Test
    void testEncoderContentTypeUsedWhenNoOverride() throws Exception {
        // encoder 显式声明非 JSON 媒体类型，且 @Body 未覆盖 → 采用 encoder 声明
        RequestTemplate xmlTemplate = new RequestTemplate(xmlEncoder);
        MethodMetadata meta = new MethodMetadata()
                .httpMethod("POST")
                .pathTemplate("/xml");
        meta.addParamBinding(new ParamBinding(ParamBinding.Kind.BODY, null, 0));

        SpiderRequest request = xmlTemplate.build(meta, new Object[]{"<x/>"}, "http://localhost:8081");

        assertEquals("application/xml", request.contentType());
    }

    @Test
    void testBuildPostWithNullBodyArgStillSetsContentType() throws Exception {
        // content-type 设置与 body 是否非空解耦：argValue=null 但声明了 contentType，仍应设置
        MethodMetadata meta = new MethodMetadata()
                .httpMethod("POST")
                .pathTemplate("/xml");
        meta.addParamBinding(new ParamBinding(ParamBinding.Kind.BODY, null, 0, "application/xml"));

        SpiderRequest request = template.build(meta, new Object[]{null}, "http://localhost:8081");

        assertEquals("application/xml", request.contentType());
        assertNull(request.body());
    }
}
