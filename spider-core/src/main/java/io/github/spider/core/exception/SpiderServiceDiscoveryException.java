package io.github.spider.core.exception;

/**
 * 服务发现/解析失败：未找到服务实例、Nacos 查询失败等。
 * 此类错误可能是暂时的，可被重试。
 */
public class SpiderServiceDiscoveryException extends SpiderException {

    public SpiderServiceDiscoveryException(String message) {
        super(message);
    }

    public SpiderServiceDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public ErrorCategory category() {
        return ErrorCategory.SERVICE_DISCOVERY;
    }
}
