package io.github.spider.grpc;

import io.github.spider.core.transport.SpiderRequest;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GrpcSpiderTransportTest {

    @Test
    void testMissingBindingThrowsIOException() {
        ManagedChannel channel = InProcessChannelBuilder.forName("test-" + System.nanoTime())
                .usePlaintext().build();
        try {
            GrpcSpiderTransport transport = new GrpcSpiderTransport(channel);
            SpiderRequest request = new SpiderRequest().method("POST").path("/nonexistent");
            assertThrows(IOException.class, () -> transport.execute(request));
        } finally {
            channel.shutdownNow();
        }
    }

    @Test
    void testImplementsSpiderTransport() {
        ManagedChannel channel = InProcessChannelBuilder.forName("test-" + System.nanoTime())
                .usePlaintext().build();
        try {
            GrpcSpiderTransport transport = new GrpcSpiderTransport(channel);
            assertTrue(transport instanceof io.github.spider.core.transport.SpiderTransport);
        } finally {
            channel.shutdownNow();
        }
    }

    @Test
    void testShutdown() throws Exception {
        ManagedChannel channel = InProcessChannelBuilder.forName("test-shutdown-" + System.nanoTime())
                .usePlaintext().build();
        GrpcSpiderTransport transport = new GrpcSpiderTransport(channel);
        transport.shutdown();
        // Should not throw
    }
}
