package io.github.spider.core.exception;

/**
 * HTTP 服务端错误（5xx）。此类错误表示服务端暂时故障，可被重试。
 */
public class SpiderHttpServerException extends SpiderHttpException {

    public SpiderHttpServerException(int statusCode, String message) {
        super(statusCode, message);
    }

    @Override
    public ErrorCategory category() {
        return ErrorCategory.HTTP_SERVER;
    }
}
