package io.github.spider.messaging;

import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryMessageTransportTest {

    @Test
    void testAsyncSendAndReply() throws Exception {
        InMemoryMessageTransport transport = new InMemoryMessageTransport();
        transport.registerHandler("/orders", request ->
                new SpiderResponse().statusCode(200).bodyBytes("\"processed\"".getBytes()));

        CountDownLatch latch = new CountDownLatch(1);
        SpiderRequest req = new SpiderRequest().method("POST").path("/orders");
        final SpiderResponse[] result = {null};

        transport.send(req, new SpiderMessageTransport.ReplyCallback() {
            @Override public void onReply(SpiderResponse response) { result[0] = response; latch.countDown(); }
            @Override public void onError(Throwable error) { latch.countDown(); }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(result[0]);
        assertEquals(200, result[0].statusCode());
    }

    @Test
    void testMissingHandlerReturnsError() throws Exception {
        InMemoryMessageTransport transport = new InMemoryMessageTransport();
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] error = {null};

        transport.send(new SpiderRequest().path("/unknown"),
                new SpiderMessageTransport.ReplyCallback() {
                    @Override public void onReply(SpiderResponse r) { latch.countDown(); }
                    @Override public void onError(Throwable t) { error[0] = t; latch.countDown(); }
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(error[0]);
    }
}
