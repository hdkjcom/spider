package io.github.spider.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spider.
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
 * </pre>
 */
@ConfigurationProperties(prefix = "spider")
public class SpiderProperties {

    /** Default timeout in milliseconds for all clients. */
    private int defaultTimeout = 5000;

    /** Default retry configuration. */
    private RetryConfig defaultRetry = new RetryConfig();

    /** Transport-level configuration. */
    private TransportConfig transport = new TransportConfig();

    public int getDefaultTimeout() { return defaultTimeout; }
    public void setDefaultTimeout(int defaultTimeout) { this.defaultTimeout = defaultTimeout; }

    public RetryConfig getDefaultRetry() { return defaultRetry; }
    public void setDefaultRetry(RetryConfig defaultRetry) { this.defaultRetry = defaultRetry; }

    public TransportConfig getTransport() { return transport; }
    public void setTransport(TransportConfig transport) { this.transport = transport; }

    public static class RetryConfig {
        private int maxAttempts = 3;
        private long backoffMillis = 100;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public long getBackoffMillis() { return backoffMillis; }
        public void setBackoffMillis(long backoffMillis) { this.backoffMillis = backoffMillis; }
    }

    public static class TransportConfig {
        private long connectTimeout = 10000;
        private long readTimeout = 30000;
        private long writeTimeout = 30000;

        public long getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(long connectTimeout) { this.connectTimeout = connectTimeout; }
        public long getReadTimeout() { return readTimeout; }
        public void setReadTimeout(long readTimeout) { this.readTimeout = readTimeout; }
        public long getWriteTimeout() { return writeTimeout; }
        public void setWriteTimeout(long writeTimeout) { this.writeTimeout = writeTimeout; }
    }
}
