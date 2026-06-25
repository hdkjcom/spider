package io.github.spider.messaging;

import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;

/**
 * 消息队列传输的 SPI（支持 Kafka、RabbitMQ 等）。
 * 基于消息基础设施实现异步请求-响应模式。
 */
public interface SpiderMessageTransport {

    /**
     * 发送请求消息并异步接收回复。
     *
     * @param request  Spider 请求对象
     * @param callback 当回复到达或超时时回调
     */
    void send(SpiderRequest request, ReplyCallback callback);

    /** 异步回复回调接口。 */
    interface ReplyCallback {
        /** 回复到达时调用。 */
        void onReply(SpiderResponse response);
        /** 发生错误时调用。 */
        void onError(Throwable error);
    }

    /** 空操作的传输实现。 */
    SpiderMessageTransport NOOP = (request, callback) ->
            callback.onError(new UnsupportedOperationException("未配置消息传输"));
}
