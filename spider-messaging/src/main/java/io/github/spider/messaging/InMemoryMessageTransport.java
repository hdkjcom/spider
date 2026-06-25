package io.github.spider.messaging;

import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 基于内存的消息传输实现，用于测试和本地开发。
 * 使用线程池实现异步请求-响应模式。
 */
public class InMemoryMessageTransport implements SpiderMessageTransport {

    /** 处理器注册表，按主题（topic）存储请求处理器。 */
    private final Map<String, ReplyHandler> handlers = new ConcurrentHashMap<>();
    /** 缓存线程池，用于异步执行消息处理。 */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 注册一个请求处理器，用于处理指定主题的请求并生成回复。
     *
     * @param topic   消息主题
     * @param handler 请求处理器
     */
    public void registerHandler(String topic, ReplyHandler handler) {
        handlers.put(topic, handler);
    }

    /**
     * 异步发送请求消息，通过线程池执行处理并回调结果。
     *
     * @param request  Spider 请求对象
     * @param callback 异步回调，用于接收回复或错误
     */
    @Override
    public void send(SpiderRequest request, ReplyCallback callback) {
        // 将请求处理提交到线程池异步执行
        executor.submit(() -> {
            try {
                // 根据请求路径查找对应的处理器
                ReplyHandler handler = handlers.get(request.path());
                if (handler == null) {
                    callback.onError(new RuntimeException("未找到处理器: " + request.path()));
                    return;
                }
                // 调用处理器生成回复并回调
                SpiderResponse response = handler.handle(request);
                callback.onReply(response);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /** 请求处理器的函数式接口。 */
    @FunctionalInterface
    public interface ReplyHandler {
        /**
         * 处理请求并生成回复。
         *
         * @param request Spider 请求对象
         * @return Spider 响应对象
         * @throws Exception 处理过程中可能抛出的异常
         */
        SpiderResponse handle(SpiderRequest request) throws Exception;
    }
}
