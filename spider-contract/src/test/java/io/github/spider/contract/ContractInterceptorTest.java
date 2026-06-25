package io.github.spider.contract;

import io.github.spider.core.transport.SpiderResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContractInterceptorTest {

    @Test
    void testValidatesSuccessfulResponse() {
        final boolean[] validated = {false};
        ContractInterceptor interceptor = new ContractInterceptor(response -> {
            validated[0] = true;
            assertEquals(200, response.statusCode());
        });

        SpiderResponse response = new SpiderResponse().statusCode(200).bodyBytes("{}".getBytes());
        interceptor.afterResponse(response);
        assertTrue(validated[0]);
    }

    @Test
    void testSkipsNonSuccessfulResponse() {
        final boolean[] validated = {false};
        ContractInterceptor interceptor = new ContractInterceptor(response -> validated[0] = true);

        SpiderResponse response = new SpiderResponse().statusCode(500);
        interceptor.afterResponse(response);
        assertFalse(validated[0]);
    }

    @Test
    void testNoOpWhenNoValidator() {
        ContractInterceptor interceptor = new ContractInterceptor(null);
        SpiderResponse response = new SpiderResponse().statusCode(200);
        // Should not throw
        SpiderResponse result = interceptor.afterResponse(response);
        assertSame(response, result);
    }
}
