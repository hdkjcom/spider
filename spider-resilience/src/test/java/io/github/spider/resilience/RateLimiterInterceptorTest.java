package io.github.spider.resilience;

import io.github.spider.core.annotation.RateLimit;
import io.github.spider.core.client.SpiderClientException;
import io.github.spider.core.transport.SpiderRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterInterceptorTest {

    @RateLimit(permits = 2, duration = 1)
    interface TestConfig {}

    @Test
    void testAllowsPermitsWithinLimit() throws Exception {
        RateLimit ann = TestConfig.class.getAnnotation(RateLimit.class);
        RateLimiterInterceptor interceptor = new RateLimiterInterceptor("test", ann);

        // First 2 calls should pass
        interceptor.beforeRequest(new SpiderRequest().method("GET").url("http://localhost"));
        interceptor.beforeRequest(new SpiderRequest().method("GET").url("http://localhost"));

        // 3rd call should throw
        assertThrows(SpiderClientException.class, () ->
                interceptor.beforeRequest(new SpiderRequest().method("GET").url("http://localhost")));
    }

    @Test
    void testRateLimitAnnotationValues() throws Exception {
        RateLimit ann = TestConfig.class.getAnnotation(RateLimit.class);
        assertEquals(2, ann.permits());
    }
}
