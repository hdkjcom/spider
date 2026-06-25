package io.github.spider.contract;

import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.transport.SpiderResponse;

/**
 * 在请求和响应上执行契约约束验证的拦截器。
 *
 * &lt;p&gt;可将其作为自定义契约验证器的基类，或以编程方式配置验证规则。
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

    /** 根据契约期望验证响应体。 */
    @FunctionalInterface
    public interface ResponseValidator {
        void validate(SpiderResponse response);
    }
}
