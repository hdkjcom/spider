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

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(SpiderProperties.class)
@ConditionalOnProperty(prefix = "spider", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpiderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OkHttpClient okHttpClient(SpiderProperties props) {
        return new OkHttpClient.Builder()
                .connectTimeout(props.getTransport().getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(props.getTransport().getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(props.getTransport().getWriteTimeout(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SpiderTransport spiderTransport(OkHttpClient client) {
        return new OkHttpSpiderTransport(client);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpiderEncoder spiderEncoder() { return new JacksonSpiderEncoder(); }

    @Bean
    @ConditionalOnMissingBean
    public SpiderDecoder spiderDecoder() { return new JacksonSpiderDecoder(); }

    @Bean
    @ConditionalOnMissingBean
    public SpiderMetrics spiderMetrics() { return SpiderMetrics.NOOP; }

    @Bean
    @ConditionalOnMissingBean
    public SpiderClientFactory spiderClientFactory(
            SpiderTransport transport, SpiderDecoder decoder, SpiderEncoder encoder,
            List<SpiderInterceptor> interceptors, SpiderMetrics metrics) {
        return SpiderClientFactory.builder()
                .transport(transport).decoder(decoder).encoder(encoder)
                .interceptors(interceptors).metrics(metrics)
                .build();
    }
}
