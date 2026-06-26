package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.client.SpiderClientFactory;
import io.github.spider.core.codec.SpiderDecoder;
import io.github.spider.core.codec.SpiderEncoder;
import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.metrics.SpiderMetrics;
import io.github.spider.core.transport.SpiderTransport;
import io.github.spider.http.OkHttpSpiderTransport;
import io.github.spider.jackson.JacksonSpiderDecoder;
import io.github.spider.jackson.JacksonSpiderEncoder;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Spider 核心自动配置类，装配 Spider 运行所需的基础 Bean。
 *
 * <p>在 {@code spider.enabled=true}（默认开启）时激活，提供 OkHttp 传输、
 * Jackson 编解码、SPI 拦截器收集、指标暴露以及 SpiderClientFactory 的统一装配。</p>
 */
@Configuration
@EnableConfigurationProperties(SpiderProperties.class)
@ConditionalOnProperty(prefix = "spider", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpiderAutoConfiguration {

    /**
     * 创建 OkHttpClient 实例，连接/读/写超时均取自 {@link SpiderProperties.TransportConfig}。
     *
     * @param props Spider 配置属性
     * @return 配置好的 OkHttpClient
     */
    @Bean
    @ConditionalOnMissingBean
    public OkHttpClient okHttpClient(SpiderProperties props) {
        return new OkHttpClient.Builder()
                .connectTimeout(props.getTransport().getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(props.getTransport().getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(props.getTransport().getWriteTimeout(), TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * 创建基于 OkHttp 的 Spider 传输实现。
     *
     * @param client OkHttpClient 实例
     * @return OkHttpSpiderTransport
     */
    @Bean
    @ConditionalOnMissingBean
    public SpiderTransport spiderTransport(OkHttpClient client) {
        return new OkHttpSpiderTransport(client);
    }

    /**
     * 创建 Jackson 编码器。
     *
     * @return JacksonSpiderEncoder 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public SpiderEncoder spiderEncoder() { return new JacksonSpiderEncoder(); }

    /**
     * 创建 Jackson 解码器。
     *
     * @return JacksonSpiderDecoder 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public SpiderDecoder spiderDecoder() { return new JacksonSpiderDecoder(); }

    /**
     * 提供默认的 NOOP 指标实现，待 spider-metrics 模块提供 Micrometer 实现时可被覆盖。
     *
     * @return NOOP SpiderMetrics
     */
    @Bean
    @ConditionalOnMissingBean
    public SpiderMetrics spiderMetrics() { return SpiderMetrics.NOOP; }

    /**
     * 装配 SpiderClientFactory，聚合传输、编解码、拦截器与指标组件。
     *
     * @param transport    Spider 传输实现
     * @param decoder      Spider 解码器
     * @param encoder      Spider 编码器
     * @param interceptors 所有 SpiderInterceptor Bean（可为空）
     * @param metrics      Spider 指标实现
     * @return 配置完成的 SpiderClientFactory
     */
    @Bean
    @ConditionalOnMissingBean
    public SpiderClientFactory spiderClientFactory(
            SpiderTransport transport, SpiderDecoder decoder, SpiderEncoder encoder,
            List<SpiderInterceptor> interceptors, SpiderMetrics metrics,
            SpiderProperties props) {
        // 将 SpiderProperties.ClientConfig 转换为通用 Map 格式（保持 core 无 Spring 依赖）
        Map<String, Map<String, Object>> clientConfigs = new HashMap<>();
        if (props.getClients() != null) {
            for (Map.Entry<String, SpiderProperties.ClientConfig> entry : props.getClients().entrySet()) {
                Map<String, Object> cfg = new HashMap<>();
                SpiderProperties.ClientConfig cc = entry.getValue();
                if (cc.getUrl() != null) cfg.put("url", cc.getUrl());
                if (cc.getTimeout() != null) cfg.put("timeout", cc.getTimeout());
                cfg.put("retry.maxAttempts", cc.getRetry().getMaxAttempts());
                cfg.put("retry.backoffMillis", cc.getRetry().getBackoffMillis());
                clientConfigs.put(entry.getKey(), cfg);
            }
        }
        return SpiderClientFactory.builder()
                .transport(transport).decoder(decoder).encoder(encoder)
                .interceptors(interceptors).metrics(metrics)
                .defaultTimeout(props.getDefaultTimeout())
                .defaultRetry(props.getDefaultRetry().getMaxAttempts(),
                        props.getDefaultRetry().getBackoffMillis())
                .clientConfigs(clientConfigs)
                .build();
    }
}
