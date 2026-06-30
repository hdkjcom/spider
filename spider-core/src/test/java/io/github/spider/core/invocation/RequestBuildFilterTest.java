package io.github.spider.core.invocation;

import io.github.spider.core.codec.SpiderEncoder;
import io.github.spider.core.metadata.MethodMetadata;
import io.github.spider.core.metadata.ParamBinding;
import io.github.spider.core.metadata.RequestTemplate;
import io.github.spider.core.transport.SpiderRequest;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link RequestBuildFilter}：
 * <ul>
 *   <li>正常构建 GET 请求</li>
 *   <li>构建 POST 请求（带 body）</li>
 *   <li>{@code config.timeout} 属性覆盖超时</li>
 *   <li>无 {@code config.timeout} 时不修改请求默认超时</li>
 * </ul>
 *
 * <p>使用真实 {@link RequestTemplate} 和 passthrough encoder（lambda {@code obj -> (byte[]) obj}），
 * 不 mock。
 */
class RequestBuildFilterTest {

    /** 透传 encoder：body 参数本身已是 byte[]，直接返回。 */
    private final SpiderEncoder passthroughEncoder = obj -> (byte[]) obj;

    @Test
    void shouldBuildGetRequest() throws Throwable {
        RequestTemplate template = new RequestTemplate(passthroughEncoder);
        RequestBuildFilter filter = new RequestBuildFilter(template);

        MethodMetadata meta = new MethodMetadata()
                .httpMethod("GET")
                .pathTemplate("/api/users");
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "testClient", null, null, meta, "http://localhost:8080");
        ctx.setResolvedBaseUrl("http://localhost:8080");

        filter.filter(ctx, new SpiderFilterChain(Collections.<SpiderInvocationFilter>emptyList()));

        SpiderRequest request = ctx.request();
        assertNotNull(request, "filter 执行后 ctx.request() 不应为 null");
        assertEquals("GET", request.method());
        assertEquals("/api/users", request.path());
        assertEquals("http://localhost:8080", request.url());
    }

    @Test
    void shouldBuildPostRequestWithBody() throws Throwable {
        RequestTemplate template = new RequestTemplate(passthroughEncoder);
        RequestBuildFilter filter = new RequestBuildFilter(template);

        MethodMetadata meta = new MethodMetadata()
                .httpMethod("POST")
                .pathTemplate("/api/users")
                .addParamBinding(new ParamBinding(ParamBinding.Kind.BODY, "body", 0));
        byte[] body = "{\"name\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "testClient", null, new Object[]{body}, meta, "http://localhost:8080");
        ctx.setResolvedBaseUrl("http://localhost:8080");

        filter.filter(ctx, new SpiderFilterChain(Collections.<SpiderInvocationFilter>emptyList()));

        SpiderRequest request = ctx.request();
        assertNotNull(request);
        assertEquals("POST", request.method());
        assertArrayEquals(body, request.body(), "POST 请求体应被 encoder 透传");
    }

    @Test
    void shouldOverrideTimeoutWhenConfigTimeoutAttributeSet() throws Throwable {
        RequestTemplate template = new RequestTemplate(passthroughEncoder);
        RequestBuildFilter filter = new RequestBuildFilter(template);

        MethodMetadata meta = new MethodMetadata()
                .httpMethod("GET")
                .pathTemplate("/api/data");
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "testClient", null, null, meta, "http://localhost:8080");
        ctx.setResolvedBaseUrl("http://localhost:8080");
        ctx.setAttribute("config.timeout", 5000);

        filter.filter(ctx, new SpiderFilterChain(Collections.<SpiderInvocationFilter>emptyList()));

        SpiderRequest request = ctx.request();
        assertNotNull(request);
        assertEquals(5000, request.timeoutMillis(),
                "config.timeout 属性应覆盖 request 的超时值");
    }

    @Test
    void shouldNotModifyDefaultTimeoutWhenNoConfigTimeoutAttribute() throws Throwable {
        RequestTemplate template = new RequestTemplate(passthroughEncoder);
        RequestBuildFilter filter = new RequestBuildFilter(template);

        // meta.timeoutMillis() 默认 -1，template 不会设置超时，request 保持默认 0
        MethodMetadata meta = new MethodMetadata()
                .httpMethod("GET")
                .pathTemplate("/api/data");
        SpiderInvocationContext ctx = new SpiderInvocationContext(
                "testClient", null, null, meta, "http://localhost:8080");
        ctx.setResolvedBaseUrl("http://localhost:8080");
        // 不设置 config.timeout 属性

        filter.filter(ctx, new SpiderFilterChain(Collections.<SpiderInvocationFilter>emptyList()));

        SpiderRequest request = ctx.request();
        assertNotNull(request);
        assertEquals(0, request.timeoutMillis(),
                "无 config.timeout 属性时不应修改 request 默认超时");
    }
}
