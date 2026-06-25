package io.github.spider.core.transport;

import java.io.IOException;

/**
 * Transport abstraction. Implementations handle the actual I/O for a protocol (HTTP, gRPC, messaging).
 *
 * <p>Spider ships with {@code OkHttpSpiderTransport} (HTTP) and {@code GrpcSpiderTransport} (gRPC).
 * Custom transports implement this interface and pass it to the factory builder.
 */
public interface SpiderTransport {
    /**
     * Execute a remote call.
     *
     * @param request transport-agnostic request model
     * @return transport-agnostic response model
     * @throws IOException on network-level failures
     */
    SpiderResponse execute(SpiderRequest request) throws IOException;
}
