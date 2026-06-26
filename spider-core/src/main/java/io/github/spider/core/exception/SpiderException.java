package io.github.spider.core.exception;

/**
 * Spider 框架所有异常的抽象基类。
 *
 * <p>每种异常都属于一个 {@link ErrorCategory}，用于分类重试决策、指标标签和追踪 span。
 * 所有子类均为非受检异常（继承自 {@link RuntimeException}）。
 */
public abstract class SpiderException extends RuntimeException {

    /**
     * 异常分类，用于程序化决策（如重试、指标标记）。
     */
    public enum ErrorCategory {
        /** 配置/bootstrap 错误，不应重试 */
        CONFIG,
        /** 服务发现/解析错误，可短暂重试 */
        SERVICE_DISCOVERY,
        /** 熔断器拒绝（熔断器开启） */
        CIRCUIT_BREAKER,
        /** 限流拒绝 */
        RATE_LIMIT,
        /** HTTP 客户端错误（4xx） */
        HTTP_CLIENT,
        /** HTTP 服务端错误（5xx） */
        HTTP_SERVER,
        /** 网络 I/O 故障 */
        NETWORK_IO,
        /** 降级执行失败 */
        FALLBACK,
        /** 契约校验失败 */
        CONTRACT
    }

    protected SpiderException(String message) {
        super(message);
    }

    protected SpiderException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 返回此异常的语义分类。
     *
     * @return 错误分类，用于重试策略、指标标签等
     */
    public abstract ErrorCategory category();
}
