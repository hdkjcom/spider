package io.github.spider.core.transport;

import io.github.spider.core.client.SpiderClientException;
import io.github.spider.core.exception.SpiderCircuitBreakerOpenException;
import io.github.spider.core.policy.SpiderCircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 带有熔断器逻辑的 SpiderTransport 装饰器。
 * 每次调用前检查熔断器状态；每次调用后记录成功或失败。
 *
 * <p>失败认定：5xx 与 IOException 计为熔断失败；4xx（鉴权/参数等客户端错误）计为成功——
 * 这类是调用方问题而非下游服务故障，不应触发熔断。若需把特定 4xx 也算失败，
 * 可自定义 {@link SpiderCircuitBreaker} 实现覆盖 recordFailure 判定。
 */
public class CircuitBreakerTransport implements SpiderTransport {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerTransport.class);

    private final SpiderTransport delegate;
    private final SpiderCircuitBreaker circuitBreaker;

    public CircuitBreakerTransport(SpiderTransport delegate, SpiderCircuitBreaker circuitBreaker) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public SpiderResponse execute(SpiderRequest request) throws IOException {
        if (!circuitBreaker.isAllowed()) {
            log.warn("熔断器已打开，拒绝请求: {}", request.fullUrl());
            throw new SpiderCircuitBreakerOpenException(request.fullUrl());
        }

        try {
            SpiderResponse response = delegate.execute(request);
            if (response.isSuccessful()) {
                circuitBreaker.recordSuccess();
            } else {
                // 5xx 计入熔断器失败统计
                if (response.statusCode() >= 500) {
                    circuitBreaker.recordFailure(
                            new SpiderClientException(response.statusCode(), "HTTP " + response.statusCode()));
                } else {
                    circuitBreaker.recordSuccess();
                }
            }
            return response;
        } catch (IOException e) {
            circuitBreaker.recordFailure(e);
            throw e;
        }
    }
}
