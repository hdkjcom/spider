package io.github.spider.core.exception;

/**
 * 响应契约校验失败：响应体不符合 {@code @ValidateResponse} 定义的契约。
 */
public class SpiderContractViolationException extends SpiderException {

    public SpiderContractViolationException(String message) {
        super(message);
    }

    public SpiderContractViolationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public ErrorCategory category() {
        return ErrorCategory.CONTRACT;
    }
}
