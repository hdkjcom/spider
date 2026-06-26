package io.github.spider.core.exception;

/**
 * HTTP 客户端错误（4xx）。此类错误表示请求方存在问题，不应被重试。
 */
public class SpiderHttpClientException extends SpiderHttpException {

    public SpiderHttpClientException(int statusCode, String message) {
        super(statusCode, message);
    }

    @Override
    public ErrorCategory category() {
        return ErrorCategory.HTTP_CLIENT;
    }
}
