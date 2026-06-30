package io.github.spider.core.client;

import io.github.spider.core.annotation.SpiderClient;
import io.github.spider.core.codec.SpiderDecoder;
import io.github.spider.core.codec.SpiderEncoder;
import io.github.spider.core.discovery.SpiderServiceDiscovery;
import io.github.spider.core.discovery.RoundRobinSpiderLoadBalancer;
import io.github.spider.core.discovery.SpiderLoadBalancer;
import io.github.spider.core.exception.SpiderConfigurationException;
import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.invocation.*;
import io.github.spider.core.metadata.DefaultMethodMetadataParser;
import io.github.spider.core.metadata.MethodMetadata;
import io.github.spider.core.metadata.RequestTemplate;
import io.github.spider.core.metrics.SpiderMetrics;
import io.github.spider.core.policy.SpiderCircuitBreaker;
import io.github.spider.core.runtime.SpiderRuntime;
import io.github.spider.core.transport.CircuitBreakerTransport;
import io.github.spider.core.transport.SpiderTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <tr><td>loadBalancer</td><td>no</td><td>instance selection strategy</td></tr>
 * </table>
 *
 * <p>In a Spring Boot application the factory is managed by the starter;
 * use {@code @EnableSpiderClients} instead of using this class directly.
 */
public class SpiderClientFactory {

    private static final Logger log = LoggerFactory.getLogger(SpiderClientFactory.class);
    private static volatile boolean bannerPrinted = false;

    static { printBanner(); }

    private static void printBanner() {
        // Banner is off by default to avoid log noise.
        // Enable explicitly with -Dspider.banner=true
        if (!Boolean.getBoolean("spider.banner")) return;
        if (bannerPrinted) return;
        bannerPrinted = true;
        try (java.io.InputStream in = SpiderClientFactory.class.getClassLoader()
                .getResourceAsStream("spider-banner.txt")) {
            if (in != null) {
                byte[] buf = new byte[4096];
                int n = in.read(buf);
                if (n > 0) log.info(new String(buf, 0, n));
            }
        } catch (Exception e) {
            // banner 读取失败不影响功能
        }
    }

    private final SpiderTransport transport;
    private final SpiderDecoder decoder;
    private final SpiderEncoder encoder;
    private final List<SpiderInterceptor> interceptors;
    private final SpiderMetrics metrics;
    private final SpiderCircuitBreaker circuitBreaker;
    private final SpiderServiceDiscovery serviceDiscovery;
    private final SpiderLoadBalancer loadBalancer;
    private final String urlOverride;
    private final int defaultTimeout;
    private final int defaultRetryMaxAttempts;
    private final long defaultRetryBackoffMillis;
    private final Map<String, Map<String, Object>> clientConfigs;
    private final List<SpiderInvocationFilter> extraFilters;

    private SpiderClientFactory(Builder builder) {
        this.transport = builder.transport;
        this.decoder = builder.decoder;
        this.encoder = builder.encoder;
        this.interceptors = new ArrayList<>(builder.interceptors);
        this.metrics = builder.metrics != null ? builder.metrics : SpiderMetrics.NOOP;
        this.circuitBreaker = builder.circuitBreaker;
        this.serviceDiscovery = builder.serviceDiscovery != null ? builder.serviceDiscovery : SpiderServiceDiscovery.NOOP;
        this.loadBalancer = builder.loadBalancer != null ? builder.loadBalancer : new RoundRobinSpiderLoadBalancer();
        this.urlOverride = builder.urlOverride;
        this.defaultTimeout = builder.defaultTimeout;
        this.defaultRetryMaxAttempts = builder.defaultRetryMaxAttempts;
        this.defaultRetryBackoffMillis = builder.defaultRetryBackoffMillis;
        this.clientConfigs = builder.clientConfigs != null ? builder.clientConfigs : Collections.<String, Map<String, Object>>emptyMap();
        this.extraFilters = new ArrayList<>(builder.extraFilters);
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> clientInterface) {
        SpiderClient ann = clientInterface.getAnnotation(SpiderClient.class);
        if (ann == null) throw new SpiderConfigurationException(clientInterface.getName() + " must be annotated with @SpiderClient");

        SpiderTransport effectiveTransport = this.transport;
        if (circuitBreaker != null) {
            effectiveTransport = new CircuitBreakerTransport(effectiveTransport, circuitBreaker);
        } else {
            io.github.spider.core.annotation.SpiderCircuitBreaker cbAnn =
                    clientInterface.getAnnotation(io.github.spider.core.annotation.SpiderCircuitBreaker.class);
            if (cbAnn != null) {
                CountingCircuitBreaker cb = new CountingCircuitBreaker(cbAnn);
                effectiveTransport = new CircuitBreakerTransport(effectiveTransport, cb);
                SpiderRuntime.getInstance().registerCircuitBreaker(ann.name(), cb);
            }
        }

        if (circuitBreaker != null) {
            SpiderRuntime.getInstance().registerCircuitBreaker(ann.name(), circuitBreaker);
        }

        String name = ann.name();
        String baseUrl = urlOverride != null ? urlOverride : ann.url();

        // 应用每客户端配置（spider.clients.<name>.* in application.yml）
        int effectiveTimeout = defaultTimeout;
        int effectiveMaxAttempts = defaultRetryMaxAttempts;
        long effectiveBackoffMillis = defaultRetryBackoffMillis;
        Map<String, Object> clientCfg = clientConfigs.get(name);
        if (clientCfg != null) {
            if (clientCfg.containsKey("url") && clientCfg.get("url") != null) {
                baseUrl = (String) clientCfg.get("url");
            }
            if (clientCfg.get("timeout") instanceof Number) {
                effectiveTimeout = ((Number) clientCfg.get("timeout")).intValue();
            }
            if (clientCfg.get("retry.maxAttempts") instanceof Number) {
                effectiveMaxAttempts = ((Number) clientCfg.get("retry.maxAttempts")).intValue();
            }
            if (clientCfg.get("retry.backoffMillis") instanceof Number) {
                effectiveBackoffMillis = ((Number) clientCfg.get("retry.backoffMillis")).longValue();
            }
        }

        Map<Method, MethodMetadata> meta = new HashMap<>();
        Map<Method, Object> fallbacks = new HashMap<>();

        DefaultMethodMetadataParser parser = new DefaultMethodMetadataParser();
        for (Method m : clientInterface.getMethods()) {
            MethodMetadata mm = parser.parse(m, clientInterface, effectiveTimeout,
                    effectiveMaxAttempts, effectiveBackoffMillis);
            if (mm != null) meta.put(m, mm);
        }

        buildFallbacks(ann, clientInterface, fallbacks);

        RequestTemplate template = new RequestTemplate(encoder);

        // 构建 filter chain（自定义过滤器在链首，先于标准过滤器执行）
        List<SpiderInvocationFilter> filters = new ArrayList<>(this.extraFilters);
        filters.add(new ResponseContextFilter());
        filters.add(new ServiceDiscoveryFilter(serviceDiscovery, loadBalancer));
        filters.add(new RequestBuildFilter(template));
        filters.add(new InterceptorFilter(interceptors));
        filters.add(new FallbackFilter(fallbacks, metrics));
        filters.add(new MetricsFilter(metrics));
        filters.add(new RetryFilter(metrics));
        filters.add(new TransportFilter(effectiveTransport));
        filters.add(new DecodeFilter(decoder));

        SpiderFilterChain chainTemplate = new SpiderFilterChain(filters);
        SpiderInvocationHandler handler = new SpiderInvocationHandler(name, baseUrl, meta, chainTemplate);

        return (T) Proxy.newProxyInstance(clientInterface.getClassLoader(), new Class[]{clientInterface}, handler);
    }

    @SuppressWarnings("unchecked")
    private void buildFallbacks(SpiderClient ann, Class<?> iface, Map<Method, Object> map) {
        Class<?> ff = ann.fallbackFactory();
        if (ff != null && ff != Void.class) {
            try {
                Object f = ff.getDeclaredConstructor().newInstance();
                for (Method m : iface.getMethods()) map.put(m, f);
            } catch (Exception e) { throw new SpiderConfigurationException("Failed to create fallbackFactory: " + ff.getName(), e); }
            return;
        }
        Class<?> fb = ann.fallback();
        if (fb == null || fb == Void.class) return;
        try {
            Object f = fb.getDeclaredConstructor().newInstance();
            for (Method m : iface.getMethods()) {
                try { fb.getMethod(m.getName(), m.getParameterTypes()); map.put(m, f); } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception e) { throw new SpiderConfigurationException("Failed to create fallback: " + fb.getName(), e); }
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
        private SpiderLoadBalancer loadBalancer;
        private String urlOverride;
        private int defaultTimeout = -1;
        private int defaultRetryMaxAttempts = 1;
        private long defaultRetryBackoffMillis = 100;
        private Map<String, Map<String, Object>> clientConfigs;
        private List<SpiderInvocationFilter> extraFilters = new ArrayList<>();

        public Builder transport(SpiderTransport t) { this.transport = t; return this; }
        public Builder decoder(SpiderDecoder d) { this.decoder = d; return this; }
        public Builder encoder(SpiderEncoder e) { this.encoder = e; return this; }
        public Builder url(String u) { this.urlOverride = u; return this; }
        public Builder circuitBreaker(SpiderCircuitBreaker cb) { this.circuitBreaker = cb; return this; }
        public Builder addInterceptor(SpiderInterceptor i) { this.interceptors.add(i); return this; }
        public Builder interceptors(List<SpiderInterceptor> list) { this.interceptors = new ArrayList<>(list); return this; }
        public Builder metrics(SpiderMetrics m) { this.metrics = m; return this; }
        public Builder serviceDiscovery(SpiderServiceDiscovery sd) { this.serviceDiscovery = sd; return this; }
        public Builder loadBalancer(SpiderLoadBalancer lb) { this.loadBalancer = lb; return this; }
        public Builder defaultTimeout(int ms) { this.defaultTimeout = ms; return this; }
        public Builder defaultRetry(int maxAttempts, long backoffMillis) {
            this.defaultRetryMaxAttempts = maxAttempts; this.defaultRetryBackoffMillis = backoffMillis; return this;
        }
        /** 设置每个客户端（按 name）的独立配置覆盖。key 为 clientName，value 为属性映射。 */
        public Builder clientConfigs(Map<String, Map<String, Object>> configs) {
            this.clientConfigs = configs; return this;
        }
        /** 插入自定义过滤器到链首（在标准过滤器之前执行）。可用于 ConfigCenter 覆盖等扩展。 */
        public Builder addFilter(SpiderInvocationFilter filter) { this.extraFilters.add(filter); return this; }

        public SpiderClientFactory build() {
            if (transport == null) throw new SpiderConfigurationException("transport is required");
            return new SpiderClientFactory(this);
        }
    }
}
