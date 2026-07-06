package io.github.spider.contract;

import io.github.spider.core.codec.SpiderDecoder;
import io.github.spider.core.exception.SpiderContractViolationException;
import io.github.spider.core.invocation.SpiderFilterChain;
import io.github.spider.core.invocation.SpiderInvocationContext;
import io.github.spider.core.metadata.MethodMetadata;
import io.github.spider.core.transport.SpiderResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContractValidationFilterTest {

    interface TestClient {
        @ValidateResponse(expectedStatus = 200, requireBody = true, requiredFields = {"user.id", "user.name"})
        void checked();

        void unchecked();
    }

    private Object runChain(String methodName, SpiderResponse response, ContractValidationFilter filter) throws Throwable {
        Method method = TestClient.class.getDeclaredMethod(methodName);
        MethodMetadata meta = null;
        SpiderInvocationContext ctx = new SpiderInvocationContext("test", method, new Object[0], meta, "http://x");
        ctx.setResponse(response);
        SpiderFilterChain chain = new SpiderFilterChain(Collections.<io.github.spider.core.invocation.SpiderInvocationFilter>emptyList());
        return filter.filter(ctx, chain);
    }

    /** 构造一个解析器：返回的 user 对象只含指定字段。 */
    private SpiderDecoder decoderWith(String... userFields) {
        return (body, type) -> {
            Map<String, Object> user = new HashMap<>();
            for (String f : userFields) {
                user.put(f, "v");
            }
            Map<String, Object> root = new HashMap<>();
            root.put("user", user);
            return root;
        };
    }

    @Test
    void passesWhenAllConstraintsMet() {
        ContractValidationFilter filter = new ContractValidationFilter(decoderWith("id", "name"));
        SpiderResponse response = new SpiderResponse().statusCode(200).bodyBytes("{}".getBytes());
        assertDoesNotThrow(() -> runChain("checked", response, filter));
    }

    @Test
    void violatesExpectedStatus() {
        ContractValidationFilter filter = new ContractValidationFilter();
        SpiderResponse response = new SpiderResponse().statusCode(500);
        assertThrows(SpiderContractViolationException.class,
                () -> runChain("checked", response, filter));
    }

    @Test
    void violatesRequireBody() {
        ContractValidationFilter filter = new ContractValidationFilter();
        SpiderResponse response = new SpiderResponse().statusCode(200).bodyBytes(new byte[0]);
        assertThrows(SpiderContractViolationException.class,
                () -> runChain("checked", response, filter));
    }

    @Test
    void violatesMissingRequiredField() {
        // user 只有 id，缺 name
        ContractValidationFilter filter = new ContractValidationFilter(decoderWith("id"));
        SpiderResponse response = new SpiderResponse().statusCode(200).bodyBytes("{}".getBytes());
        assertThrows(SpiderContractViolationException.class,
                () -> runChain("checked", response, filter));
    }

    @Test
    void skipsWhenNoAnnotation() {
        ContractValidationFilter filter = new ContractValidationFilter();
        // unchecked 方法无 @ValidateResponse，即便响应"违反"也不校验
        SpiderResponse response = new SpiderResponse().statusCode(500);
        assertDoesNotThrow(() -> runChain("unchecked", response, filter));
    }

    @Test
    void noDecoderSkipsFieldCheck() {
        // 无 decoder：requiredFields 被跳过，但 expectedStatus / requireBody 仍校验
        ContractValidationFilter filter = new ContractValidationFilter();
        // 200 + 非空 body 满足状态码与空体约束；requiredFields 因无 decoder 不检查
        SpiderResponse response = new SpiderResponse().statusCode(200).bodyBytes("{}".getBytes());
        assertDoesNotThrow(() -> runChain("checked", response, filter));
    }
}
