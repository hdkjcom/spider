package io.github.spider.core.exception;

/**
 * 配置/bootstrap 错误：缺少注解、缺少传输、解码器未配置、降级创建失败等。
 * 此类错误是永久性的，永远不应被重试。
 */
public class SpiderConfigurationException extends SpiderException {

    public SpiderConfigurationException(String message) {
        super(message);
    }

    public SpiderConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public ErrorCategory category() {
        return ErrorCategory.CONFIG;
    }
}
