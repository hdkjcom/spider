package io.github.spider.core.client;

/**
 * Spider 远程调用失败时抛出的异常。
 * statusCode 为 -1 表示非 HTTP 错误（如网络异常），其他值表示 HTTP 状态码。
 */
public class SpiderClientException extends RuntimeException {

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
}
