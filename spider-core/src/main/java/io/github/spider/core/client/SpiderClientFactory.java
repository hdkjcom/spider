package io.github.spider.core.client;

import io.github.spider.core.annotation.SpiderClient;
import io.github.spider.core.codec.SpiderDecoder;
import io.github.spider.core.codec.SpiderEncoder;
import io.github.spider.core.discovery.SpiderServiceDiscovery;
import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.metadata.DefaultMethodMetadataParser;
import io.github.spider.core.metadata.MethodMetadata;
import io.github.spider.core.metadata.RequestTemplate;
import io.github.spider.core.metrics.SpiderMetrics;
import io.github.spider.core.policy.FallbackFactory;
import io.github.spider.core.policy.SpiderCircuitBreaker;
import io.github.spider.core.runtime.SpiderRuntime;
import io.github.spider.core.transport.CircuitBreakerTransport;
import io.github.spider.core.transport.SpiderTransport;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Entry point for creating Spider client proxies.
 *
 * <pre>{@code
 * UserClient client = SpiderClientFactory.builder()
 *         .transport(new OkHttpSpiderTransport())
 *         .decoder(new JacksonSpiderDecoder())
 *         .encoder(new JacksonSpiderEncoder())
 *         .build()
 *         .create(UserClient.class);
 *
 * UserDTO user = client.getUser(1L);
 * }</pre>
 *
 * <h3>Builder options</h3>
 * <table>
 * <tr><th>Method</th><th>Required</th><th>Purpose</th></tr>
 * <tr><td>transport</td><td>yes</td><td>HTTP/gRPC/messaging transport</td></tr>
 * <tr><td>decoder</td><td>for non-void returns</td><td>response body decoder</td></tr>
 * <tr><td>encoder</td><td>for POST/PUT with @Body</td><td>request body encoder</td></tr>
 * <tr><td>url</td><td>no (overrides annotation)</td><td>runtime URL override</td></tr>
 * <tr><td>circuitBreaker</td><td>no</td><td>failure protection</td></tr>
 * <tr><td>addInterceptor</td><td>no</td><td>request/response hooks</td></tr>
 * <tr><td>metrics</td><td>no</td><td>Micrometer integration</td></tr>
 * <tr><td>serviceDiscovery</td><td>no</td><td>dynamic service resolution</td></tr>
 * </table>
 *
 * <p>In a Spring Boot application the factory is managed by the starter;
 * use {@code @EnableSpiderClients} instead of using this class directly.
 */
public class SpiderClientFactory {

    private static volatile boolean bannerPrinted = false;

    static { printBanner(); }

    private static void printBanner() {
        if (bannerPrinted) return;
        bannerPrinted = true;
        try (java.io.InputStream in = SpiderClientFactory.class.getClassLoader()
                .getResourceAsStream("spider-banner.txt")) {
            if (in != null) {
                byte[] buf = new byte[4096];
                int n = in.read(buf);
                if (n > 0) System.out.println(new String(buf, 0, n));
            }
        } catch (Exception ignored) {}
    }

    private final SpiderTransport transport;
    private final SpiderDecoder decoder;
    private final SpiderEncoder encoder;
    private final List<SpiderInterceptor> interceptors;
    private final SpiderMetrics metrics;
    private final SpiderCircuitBreaker circuitBreaker;
    private final SpiderServiceDiscovery serviceDiscovery;
    private final String urlOverride;

    private SpiderClientFactory(Builder builder) {
        this.transport = builder.transport;
        this.decoder = builder.decoder;
        this.encoder = builder.encoder;
        this.interceptors = new ArrayList<>(builder.interceptors);
        this.metrics = builder.metrics != null ? builder.metrics : SpiderMetrics.NOOP;
        this.circuitBreaker = builder.circuitBreaker;
        this.serviceDiscovery = builder.serviceDiscovery != null ? builder.serviceDiscovery : SpiderServiceDiscovery.NOOP;
        this.urlOverride = builder.urlOverride;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> clientInterface) {
        SpiderClient ann = clientInterface.getAnnotation(SpiderClient.class);
        if (ann == null) throw new SpiderClientException(clientInterface.getName() + " must be annotated with @SpiderClient");

        SpiderTransport transport = this.transport;
        if (circuitBreaker != null) {
            transport = new CircuitBreakerTransport(transport, circuitBreaker);
        } else {
            io.github.spider.core.annotation.SpiderCircuitBreaker cbAnn =
                    clientInterface.getAnnotation(io.github.spider.core.annotation.SpiderCircuitBreaker.class);
            if (cbAnn != null) {
                CountingCircuitBreaker cb = new CountingCircuitBreaker(cbAnn);
                transport = new CircuitBreakerTransport(transport, cb);
                SpiderRuntime.getInstance().registerCircuitBreaker(ann.name(), cb);
            }
        }

        if (circuitBreaker != null) {
            SpiderRuntime.getInstance().registerCircuitBreaker(ann.name(), circuitBreaker);
        }

        String name = ann.name();
        String baseUrl = urlOverride != null ? urlOverride : ann.url();
        Map<Method, MethodMetadata> meta = new HashMap<>();
        Map<Method, Object> fallbacks = new HashMap<>();

        DefaultMethodMetadataParser parser = new DefaultMethodMetadataParser();
        for (Method m : clientInterface.getMethods()) {
            MethodMetadata mm = parser.parse(m);
            if (mm != null) meta.put(m, mm);
        }

        buildFallbacks(ann, clientInterface, fallbacks);

        RequestTemplate template = new RequestTemplate(encoder);
        SpiderInvocationHandler handler = new SpiderInvocationHandler(name, baseUrl, meta, template, transport, decoder, interceptors, fallbacks, metrics);

        return (T) Proxy.newProxyInstance(clientInterface.getClassLoader(), new Class[]{clientInterface}, handler);
    }

    @SuppressWarnings("unchecked")
    private void buildFallbacks(SpiderClient ann, Class<?> iface, Map<Method, Object> map) {
        Class<?> ff = ann.fallbackFactory();
        if (ff != null && ff != Void.class) {
            try {
                Object f = ff.getDeclaredConstructor().newInstance();
                for (Method m : iface.getMethods()) map.put(m, f);
            } catch (Exception e) { throw new SpiderClientException("Failed to create fallbackFactory: " + ff.getName(), e); }
            return;
        }
        Class<?> fb = ann.fallback();
        if (fb == null || fb == Void.class) return;
        try {
            Object f = fb.getDeclaredConstructor().newInstance();
            for (Method m : iface.getMethods()) {
                try { fb.getMethod(m.getName(), m.getParameterTypes()); map.put(m, f); } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception e) { throw new SpiderClientException("Failed to create fallback: " + fb.getName(), e); }
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private SpiderTransport transport;
        private SpiderDecoder decoder;
        private SpiderEncoder encoder;
        private List<SpiderInterceptor> interceptors = new ArrayList<>();
        private SpiderMetrics metrics;
        private SpiderCircuitBreaker circuitBreaker;
        private SpiderServiceDiscovery serviceDiscovery;
        private String urlOverride;

        public Builder transport(SpiderTransport t) { this.transport = t; return this; }
        public Builder decoder(SpiderDecoder d) { this.decoder = d; return this; }
        public Builder encoder(SpiderEncoder e) { this.encoder = e; return this; }
        public Builder url(String u) { this.urlOverride = u; return this; }
        public Builder circuitBreaker(SpiderCircuitBreaker cb) { this.circuitBreaker = cb; return this; }
        public Builder addInterceptor(SpiderInterceptor i) { this.interceptors.add(i); return this; }
        public Builder interceptors(List<SpiderInterceptor> list) { this.interceptors = new ArrayList<>(list); return this; }
        public Builder metrics(SpiderMetrics m) { this.metrics = m; return this; }
        public Builder serviceDiscovery(SpiderServiceDiscovery sd) { this.serviceDiscovery = sd; return this; }

        public SpiderClientFactory build() {
            if (transport == null) throw new SpiderClientException("transport is required");
            return new SpiderClientFactory(this);
        }
    }
}
