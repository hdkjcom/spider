package io.github.spider.core.runtime.dto;

/**
 * Error entry DTO for recent invocation errors.
 */
public class ErrorEntryDto {

    private String client;
    private String method;
    private String message;
    private String errorType;
    private long time;

    public ErrorEntryDto() {
    }

    public ErrorEntryDto(String client, String method, String message, String errorType, long time) {
        this.client = client;
        this.method = method;
        this.message = message;
        this.errorType = errorType;
        this.time = time;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
