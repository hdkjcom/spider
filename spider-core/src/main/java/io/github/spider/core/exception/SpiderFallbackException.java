package io.github.spider.core.exception;

/**
 * 降级执行失败：降级方法调用中发生了异常。
 */
public class SpiderFallbackException extends SpiderException {

    public SpiderFallbackException(String message) {
        super(message);
    }

    public SpiderFallbackException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public ErrorCategory category() {
        return ErrorCategory.FALLBACK;
    }
}
