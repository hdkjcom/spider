package io.github.spider.contract;

import io.github.spider.core.codec.SpiderDecoder;
import io.github.spider.core.exception.SpiderContractViolationException;
import io.github.spider.core.invocation.SpiderFilterChain;
import io.github.spider.core.invocation.SpiderInvocationContext;
import io.github.spider.core.invocation.SpiderInvocationFilter;
import io.github.spider.core.transport.SpiderResponse;

import java.util.Map;

/**
 * 契约校验过滤器：响应返回后依据方法上的 {@link ValidateResponse} 注解做实质校验。
 *
 * <p>与 {@link ContractInterceptor}（手动注册、用户自定义校验逻辑、拿不到 Method）不同，
 * 本过滤器是 <b>注解驱动</b> 的——自动读取方法上的 {@code @ValidateResponse}，对：
 * <ul>
 *   <li>{@link ValidateResponse#expectedStatus()} 期望状态码</li>
 *   <li>{@link ValidateResponse#requireBody()} 响应体非空</li>
 *   <li>{@link ValidateResponse#requiredFields()} 必填字段路径（点号分隔，如 {@code user.id}）</li>
 * </ul>
 * 逐项校验，违反时抛出 {@link SpiderContractViolationException}。
 *
 * <p>本过滤器位于调用链最外层（标准过滤器之前，作为 extra filter 注册），契约违反发生在
 * 重试循环之外，因此 <b>不会触发重试</b>——契约违反是确定性错误，重试无意义。
 *
 * <p>{@code requiredFields} 校验需要 JSON 解析，通过 {@link SpiderDecoder} 完成（通常传入
 * {@code JacksonSpiderDecoder}）。未提供 decoder 时跳过字段校验，状态码与空体校验仍生效。
 */
public class ContractValidationFilter implements SpiderInvocationFilter {

    private final SpiderDecoder decoder;

    /** 创建契约校验过滤器，不带 decoder（跳过 requiredFields 字段校验）。 */
    public ContractValidationFilter() {
        this(null);
    }

    /**
     * 创建契约校验过滤器。
     *
     * @param decoder 用于解析响应体检查必填字段的解码器，{@code null} 则跳过字段校验
     */
    public ContractValidationFilter(SpiderDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public Object filter(SpiderInvocationContext ctx, SpiderFilterChain chain) throws Throwable {
        Object result = chain.next(ctx);
        ValidateResponse ann = ctx.method().getAnnotation(ValidateResponse.class);
        if (ann == null) {
            return result;
        }
        SpiderResponse response = ctx.response();
        if (response == null) {
            return result;
        }
        validate(ann, response);
        return result;
    }

    private void validate(ValidateResponse ann, SpiderResponse response) {
        int status = response.statusCode();
        if (ann.expectedStatus() > 0 && status != ann.expectedStatus()) {
            throw new SpiderContractViolationException(
                    "Contract violation: expected status " + ann.expectedStatus() + " but got " + status);
        }
        byte[] body = response.bodyBytes();
        if (ann.requireBody() && (body == null || body.length == 0)) {
            throw new SpiderContractViolationException(
                    "Contract violation: response body required but empty");
        }
        String[] requiredFields = ann.requiredFields();
        if (requiredFields.length > 0 && (body == null || body.length == 0)) {
            throw new SpiderContractViolationException(
                    "Contract violation: requiredFields configured but response body is empty");
        }
        if (requiredFields.length > 0 && decoder != null) {
            Object parsed;
            try {
                parsed = decoder.decode(body, Object.class);
            } catch (Exception e) {
                throw new SpiderContractViolationException(
                        "Contract violation: failed to parse response body for field validation", e);
            }
            for (String path : requiredFields) {
                if (!hasField(parsed, path)) {
                    throw new SpiderContractViolationException(
                            "Contract violation: missing required field '" + path + "'");
                }
            }
        }
    }

    /**
     * 按点号路径检查嵌套字段是否存在。
     * 例如 {@code user.address.city} 要求 parsed 是 Map，逐层下探均存在。
     */
    @SuppressWarnings("unchecked")
    private boolean hasField(Object parsed, String path) {
        Object current = parsed;
        for (String key : path.split("\\.")) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
                if (current == null) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }
}
