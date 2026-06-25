package io.github.spider.core.transport;

import io.github.spider.core.client.SpiderClientException;
import io.github.spider.core.policy.SpiderCircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Decorates a SpiderTransport with circuit breaker logic.
 * Before each call, checks the circuit breaker; after each call, records success/failure.
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
            log.warn("CircuitBreaker OPEN, rejecting request: {}", request.fullUrl());
            throw new SpiderClientException("Circuit breaker is OPEN for " + request.fullUrl());
        }

        try {
            SpiderResponse response = delegate.execute(request);
            if (response.isSuccessful()) {
                circuitBreaker.recordSuccess();
            } else {
                // 5xx counts as failure for circuit breaker
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
