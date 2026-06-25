package io.github.spider.core.client;

import io.github.spider.core.transport.SpiderResponse;

/**
 * 提供响应元数据访问（响应头、状态码），在 Spider 调用后通过 ThreadLocal 获取。
 * 适用于 thread-per-request 模型，每个请求线程独立。
 *
 * <pre>{@code
 * UserDTO user = client.getUser(1L);
 * SpiderResponse resp = SpiderResponseContext.lastResponse();
 * String requestId = resp.headers().get("X-Request-Id").get(0);
 * }</pre>
 *
 * <p>在响应式或异步代码中，调用者必须在调用后、切换线程前立即捕获响应。
 */
public final class SpiderResponseContext {

    private static final ThreadLocal<SpiderResponse> CURRENT = new ThreadLocal<>();

    /** 由框架在每次传输执行后调用。 */
    public static void set(SpiderResponse response) { CURRENT.set(response); }

    /** 返回当前线程上最近一次 Spider 调用的响应，可能为 null。 */
    public static SpiderResponse lastResponse() { return CURRENT.get(); }

    /** 由框架在每次调用后调用，清理 ThreadLocal。 */
    public static void clear() { CURRENT.remove(); }

    private SpiderResponseContext() {}
}
