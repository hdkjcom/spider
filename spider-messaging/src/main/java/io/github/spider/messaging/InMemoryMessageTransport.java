package io.github.spider.messaging;

import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;

import java.util.Map;
import java.util.concurrent.*;

/**
 * In-memory message transport for testing and local development.
 * Uses a BlockingQueue for async request-reply.
 */
public class InMemoryMessageTransport implements SpiderMessageTransport {

    private final Map<String, ReplyHandler> handlers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /** Register a handler that processes requests and produces replies. */
    public void registerHandler(String topic, ReplyHandler handler) {
        handlers.put(topic, handler);
    }

    @Override
    public void send(SpiderRequest request, ReplyCallback callback) {
        executor.submit(() -> {
            try {
                ReplyHandler handler = handlers.get(request.path());
                if (handler == null) {
                    callback.onError(new RuntimeException("No handler for: " + request.path()));
                    return;
                }
                SpiderResponse response = handler.handle(request);
                callback.onReply(response);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /** Functional interface for request processing. */
    @FunctionalInterface
    public interface ReplyHandler {
        SpiderResponse handle(SpiderRequest request) throws Exception;
    }
}
