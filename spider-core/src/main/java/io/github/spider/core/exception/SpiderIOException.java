package io.github.spider.core.exception;

import java.io.IOException;

/**
 * 网络 I/O 异常：连接超时、读超时、DNS 解析失败等。
 * 此类错误通常是暂时的，可被重试。
 */
public class SpiderIOException extends SpiderException {

    private final IOException ioException;

    public SpiderIOException(String message, IOException cause) {
        super(message, cause);
        this.ioException = cause;
    }

    /** 返回被包装的原始 IOException。 */
    public IOException ioException() {
        return ioException;
    }

    @Override
    public ErrorCategory category() {
        return ErrorCategory.NETWORK_IO;
    }
}
