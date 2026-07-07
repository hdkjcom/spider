package io.github.spider.core.exception;

/**
 * 响应体解码失败时抛出（如服务端返回 200 + 非 JSON 内容）。
 *
 * <p>属于 {@link ErrorCategory#DECODE}，是确定性错误——重试无意义，
 * {@code RetryFilter} 不会重试此类异常。
 */
public class SpiderDecodeException extends SpiderException {

    public SpiderDecodeException(String message) {
        super(message);
    }

    public SpiderDecodeException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public ErrorCategory category() {
        return ErrorCategory.DECODE;
    }
}
