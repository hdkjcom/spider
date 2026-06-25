package io.github.spider.core.client;

/**
 * Exception thrown when a Spider remote call fails.
 */
public class SpiderClientException extends RuntimeException {

    private final int statusCode;

    public SpiderClientException(String message) {
        super(message);
        this.statusCode = -1;
    }

    public SpiderClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public SpiderClientException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() { return statusCode; }
}
