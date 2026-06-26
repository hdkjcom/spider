package io.github.spider.core.client;

import io.github.spider.core.exception.SpiderException;

/**
 * Spider 远程调用失败时抛出的异常。
 *
 * <p>保留此类是为了向后兼容。新代码中 HTTP 响应错误应使用
 * {@link io.github.spider.core.exception.SpiderHttpException} 及其子类
 * {@link io.github.spider.core.exception.SpiderHttpClientException} 和
 * {@link io.github.spider.core.exception.SpiderHttpServerException}。
 *
 * <p>非 HTTP 错误应使用更具体的异常类型，如
 * {@link io.github.spider.core.exception.SpiderConfigurationException}、
 * {@link io.github.spider.core.exception.SpiderCircuitBreakerOpenException} 等。
 */
public class SpiderClientException extends SpiderException {

    /** HTTP 状态码，-1 表示非 HTTP 错误 */
    private final int statusCode;

    /** 非 HTTP 错误（网络异常、超时等） */
    public SpiderClientException(String message) {
        super(message);
        this.statusCode = -1;
    }

    /** 带原始异常的非 HTTP 错误 */
    public SpiderClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    /** HTTP 响应非 2xx 时抛出的异常 */
    public SpiderClientException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() { return statusCode; }

    @Override
    public SpiderException.ErrorCategory category() {
        if (statusCode >= 400 && statusCode < 500) {
            return ErrorCategory.HTTP_CLIENT;
        }
        if (statusCode >= 500 && statusCode < 600) {
            return ErrorCategory.HTTP_SERVER;
        }
        return ErrorCategory.CONFIG;
    }
}
