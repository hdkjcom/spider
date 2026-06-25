package io.github.spider.messaging;

import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;

/**
 * SPI for message-queue-based transport (Kafka, RabbitMQ, etc.).
 * Implements async request-reply pattern over messaging infrastructure.
 */
public interface SpiderMessageTransport {

    /**
     * Send a request message and receive a reply asynchronously.
     *
     * @param request the Spider request
     * @param callback invoked when the reply arrives or on timeout
     */
    void send(SpiderRequest request, ReplyCallback callback);

    /** Callback for async reply. */
    interface ReplyCallback {
        void onReply(SpiderResponse response);
        void onError(Throwable error);
    }

    /** No-op transport. */
    SpiderMessageTransport NOOP = (request, callback) ->
            callback.onError(new UnsupportedOperationException("No message transport configured"));
}
