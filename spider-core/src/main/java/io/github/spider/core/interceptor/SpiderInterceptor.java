package io.github.spider.core.interceptor;

import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;

/**
 * Spider 请求/响应管道的拦截器接口。
 *
 * <p>拦截器按注册顺序调用。用于横切关注点：
 * 认证请求头、日志、链路追踪、指标采集等。
 *
 * <pre>{@code
 * SpiderClientFactory.builder()
 *     .addInterceptor(new SpiderInterceptor() {
 *         public SpiderRequest beforeRequest(SpiderRequest req) {
 *             req.addHeader("Authorization", token());
 *             return req;
 *         }
 *     })
 *     .build();
 * }</pre>
 */
public interface SpiderInterceptor {

    /**
     * 在传输执行之前调用。
     * 返回（可能修改后的）请求。抛出异常可中止调用。
     */
    default SpiderRequest beforeRequest(SpiderRequest request) { return request; }

    /**
     * 在传输返回成功响应之后调用。
     * 返回（可能修改后的）响应。
     */
    default SpiderResponse afterResponse(SpiderResponse response) { return response; }

    /**
     * 当请求在所有重试耗尽后仍失败时调用。
     * 返回 {@code true} 可抑制异常（调用方收到 null）。
     */
    default boolean onError(SpiderRequest request, Exception ex) { return false; }
}
