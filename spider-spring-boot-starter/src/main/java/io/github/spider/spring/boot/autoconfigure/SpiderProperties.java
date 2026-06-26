package io.github.spider.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Spider 配置属性，对应 {@code spider.*} 前缀。
 *
 * <pre>
 * spider:
 *   default-timeout: 5000
 *   default-retry:
 *     max-attempts: 3
 *     backoff-millis: 100
 *   transport:
 *     connect-timeout: 10s
 *     read-timeout: 30s
 *   clients:
 *     user-service:
 *       url: http://user:8081
 *       timeout: 2000
 *       retry:
 *         max-attempts: 5
 * </pre>
 */
@ConfigurationProperties(prefix = "spider")
public class SpiderProperties {

    /** 默认超时时间（毫秒），适用于所有客户端。 */
    private int defaultTimeout = 5000;

    /** 默认重试配置。 */
    private RetryConfig defaultRetry = new RetryConfig();

    /** 传输层配置。 */
    private TransportConfig transport = new TransportConfig();

    /** 每个客户端的独立配置，key 为 @SpiderClient.name()。 */
    private Map<String, ClientConfig> clients = new HashMap<>();

    /** 获取默认超时时间（毫秒）。 */
    public int getDefaultTimeout() { return defaultTimeout; }
    /** 设置默认超时时间（毫秒）。 */
    public void setDefaultTimeout(int defaultTimeout) { this.defaultTimeout = defaultTimeout; }

    /** 获取默认重试配置。 */
    public RetryConfig getDefaultRetry() { return defaultRetry; }
    /** 设置默认重试配置。 */
    public void setDefaultRetry(RetryConfig defaultRetry) { this.defaultRetry = defaultRetry; }

    /** 获取传输层配置。 */
    public TransportConfig getTransport() { return transport; }
    /** 设置传输层配置。 */
    public void setTransport(TransportConfig transport) { this.transport = transport; }

    /** 获取每个客户端的独立配置。 */
    public Map<String, ClientConfig> getClients() { return clients; }
    /** 设置每个客户端的独立配置。 */
    public void setClients(Map<String, ClientConfig> clients) { this.clients = clients; }

    /**
     * 重试策略配置。
     */
    public static class RetryConfig {
        /** 最大重试次数。 */
        private int maxAttempts = 3;
        /** 退避间隔（毫秒）。 */
        private long backoffMillis = 100;

        /** 获取最大重试次数。 */
        public int getMaxAttempts() { return maxAttempts; }
        /** 设置最大重试次数。 */
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        /** 获取退避间隔（毫秒）。 */
        public long getBackoffMillis() { return backoffMillis; }
        /** 设置退避间隔（毫秒）。 */
        public void setBackoffMillis(long backoffMillis) { this.backoffMillis = backoffMillis; }
    }

    /**
     * 传输层配置（连接超时、读超时、写超时）。
     */
    public static class TransportConfig {
        /** 连接超时（毫秒）。 */
        private long connectTimeout = 10000;
        /** 读超时（毫秒）。 */
        private long readTimeout = 30000;
        /** 写超时（毫秒）。 */
        private long writeTimeout = 30000;

        /** 获取连接超时（毫秒）。 */
        public long getConnectTimeout() { return connectTimeout; }
        /** 设置连接超时（毫秒）。 */
        public void setConnectTimeout(long connectTimeout) { this.connectTimeout = connectTimeout; }
        /** 获取读超时（毫秒）。 */
        public long getReadTimeout() { return readTimeout; }
        /** 设置读超时（毫秒）。 */
        public void setReadTimeout(long readTimeout) { this.readTimeout = readTimeout; }
        /** 获取写超时（毫秒）。 */
        public long getWriteTimeout() { return writeTimeout; }
        /** 设置写超时（毫秒）。 */
        public void setWriteTimeout(long writeTimeout) { this.writeTimeout = writeTimeout; }
    }

    /**
     * 单个客户端的独立配置，key 为 {@code @SpiderClient.name()}。
     * 所有字段均为可选——未设置的字段不覆盖注解默认值。
     */
    public static class ClientConfig {
        /** 覆盖 {@code @SpiderClient.url()}。 */
        private String url;
        /** 覆盖客户端级超时（毫秒）。 */
        private Integer timeout;
        /** 覆盖客户端重试配置。 */
        private RetryConfig retry = new RetryConfig();

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public Integer getTimeout() { return timeout; }
        public void setTimeout(Integer timeout) { this.timeout = timeout; }
        public RetryConfig getRetry() { return retry; }
        public void setRetry(RetryConfig retry) { this.retry = retry; }
    }
}
