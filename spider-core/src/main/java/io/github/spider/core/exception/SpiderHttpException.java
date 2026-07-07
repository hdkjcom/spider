package io.github.spider.core.exception;

import java.nio.charset.StandardCharsets;

/**
 * HTTP 响应错误基类。携带 HTTP 状态码与响应体（诊断用）。
 */
public class SpiderHttpException extends SpiderException {

    private final int statusCode;
    private final byte[] body;

    public SpiderHttpException(int statusCode, String message) {
        this(statusCode, message, null);
    }

    public SpiderHttpException(int statusCode, String message, byte[] body) {
        super(message);
        this.statusCode = statusCode;
        this.body = body;
    }

    /** 返回 HTTP 状态码。 */
    public int statusCode() {
        return statusCode;
    }

    /** 返回响应体字节（诊断用，可能为 null）。 */
    public byte[] getBody() {
        return body;
    }

    /** 返回响应体的 UTF-8 字符串（诊断用，body 为 null 时返回空串）。 */
    public String getBodyAsString() {
        return body != null ? new String(body, StandardCharsets.UTF_8) : "";
    }

    @Override
    public ErrorCategory category() {
        if (statusCode >= 400 && statusCode < 500) {
            return ErrorCategory.HTTP_CLIENT;
        }
        return ErrorCategory.HTTP_SERVER;
    }
}
