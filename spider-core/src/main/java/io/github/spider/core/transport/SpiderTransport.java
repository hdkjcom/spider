package io.github.spider.core.transport;

import java.io.IOException;

/**
 * 传输抽象。实现类负责具体协议的 I/O（HTTP、gRPC、消息队列等）。
 *
 * <p>Spider 内置 {@code OkHttpSpiderTransport}（HTTP）和 {@code GrpcSpiderTransport}（gRPC）。
 * 自定义传输实现此接口并传递给工厂建造器即可。
 */
public interface SpiderTransport {
    /**
     * 执行远程调用。
     *
     * @param request 传输无关的请求模型
     * @return 传输无关的响应模型
     * @throws IOException 网络层失败时抛出
     */
    SpiderResponse execute(SpiderRequest request) throws IOException;
}
