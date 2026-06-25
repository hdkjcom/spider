package io.github.spider.contract;

import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;

/**
 * An interceptor that validates contract constraints on requests and responses.
 *
 * <p>Use this as a base class for custom contract validators, or configure validation rules programmatically.
 */
public class ContractInterceptor implements SpiderInterceptor {

    private final ResponseValidator responseValidator;

    public ContractInterceptor(ResponseValidator responseValidator) {
        this.responseValidator = responseValidator;
    }

    @Override
    public SpiderResponse afterResponse(SpiderResponse response) {
        if (responseValidator != null && response.isSuccessful()) {
            responseValidator.validate(response);
        }
        return response;
    }

    /** Validates a response body against contract expectations. */
    @FunctionalInterface
    public interface ResponseValidator {
        void validate(SpiderResponse response);
    }
}
