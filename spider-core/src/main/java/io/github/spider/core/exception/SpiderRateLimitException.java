package io.github.spider.core.exception;

/**
 * 限流拒绝：请求被限流器拦截。
 * 此类错误一般不应被重试，或应在限流窗口结束后再重试。
 */
public class SpiderRateLimitException extends SpiderException {

    private final String requestUrl;

    public SpiderRateLimitException(String requestUrl) {
        super("Rate limit exceeded for " + requestUrl);
        this.requestUrl = requestUrl;
    }

    /** 返回被限流的请求 URL。 */
    public String requestUrl() {
        return requestUrl;
    }

    @Override
    public ErrorCategory category() {
        return ErrorCategory.RATE_LIMIT;
    }
}
