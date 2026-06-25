package io.github.spider.grpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import io.github.spider.grpc.proto.*;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end gRPC test using proto-generated stubs.
 * Starts an in-process gRPC server with GreeterImpl, calls via GrpcSpiderTransport, verifies response.
 */
class GrpcE2ETest {

    private Server grpcServer;
    private GrpcSpiderTransport transport;
    private String serverName;

    @BeforeEach
    void setUp() throws IOException {
        serverName = "e2e-" + System.nanoTime();

        // Start a real gRPC server with Greeter service
        grpcServer = InProcessServerBuilder.forName(serverName)
                .addService(new GreeterImpl())
                .build()
                .start();

        // Build transport with in-process channel
        ManagedChannel channel = InProcessChannelBuilder.forName(serverName)
                .usePlaintext().build();

        transport = new GrpcSpiderTransport(channel);

        // Get descriptors from generated proto classes
        Descriptors.Descriptor requestDesc = HelloRequest.getDescriptor();
        Descriptors.Descriptor responseDesc = HelloResponse.getDescriptor();

        // Create MethodDescriptor from generated Grpc class
        io.grpc.MethodDescriptor<DynamicMessage, DynamicMessage> methodDesc =
                GreeterGrpc.getSayHelloMethod().toBuilder(
                        io.grpc.protobuf.ProtoUtils.marshaller(
                                DynamicMessage.newBuilder(requestDesc).buildPartial()),
                        io.grpc.protobuf.ProtoUtils.marshaller(
                                DynamicMessage.newBuilder(responseDesc).buildPartial()))
                        .build();

        transport.registerMethod("/greet", "greet.Greeter", "SayHello",
                methodDesc, requestDesc, responseDesc);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (transport != null) transport.shutdown();
        if (grpcServer != null) grpcServer.shutdown();
    }

    @Test
    void testGrpcE2E() throws Exception {
        SpiderRequest request = new SpiderRequest()
                .method("POST")
                .path("/greet")
                .body("{\"name\":\"world\"}".getBytes());

        SpiderResponse response = transport.execute(request);

        assertNotNull(response);
        assertEquals(200, response.statusCode());
        String body = new String(response.bodyBytes());
        assertTrue(body.contains("Hello"), "Response should contain greeting: " + body);
        assertTrue(body.contains("world"), "Response should contain name: " + body);
    }

    // ---- gRPC service implementation ----

    private static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            HelloResponse response = HelloResponse.newBuilder()
                    .setMessage("Hello, " + request.getName() + "!")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
