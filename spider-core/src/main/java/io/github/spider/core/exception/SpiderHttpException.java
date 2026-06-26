package io.github.spider.core.exception;

/**
 * HTTP 响应错误基类。携带 HTTP 状态码。
 */
public class SpiderHttpException extends SpiderException {

    private final int statusCode;

    public SpiderHttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /** 返回 HTTP 状态码。 */
    public int statusCode() {
        return statusCode;
    }

    @Override
    public ErrorCategory category() {
        if (statusCode >= 400 && statusCode < 500) {
            return ErrorCategory.HTTP_CLIENT;
        }
        return ErrorCategory.HTTP_SERVER;
    }
}
