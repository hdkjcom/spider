package io.github.spider.core.policy;

/**
 * 降级实例工厂，可获取失败原因以创建不同的降级对象。
 *
 * <pre>{@code
 * public class PayClientFallbackFactory implements FallbackFactory<PayClient> {
 *     public PayClient create(Throwable cause) {
 *         log.warn("降级触发: {}", cause.getMessage());
 *         return id -> PayResult.empty(id);
 *     }
 * }
 *
 * @SpiderClient(name = "pay", url = "...", fallbackFactory = PayClientFallbackFactory.class)
 * }</pre>
 *
 * @param <T> 客户端接口类型
 */
public interface FallbackFactory<T> {
    /** 根据给定的失败原因创建降级实例。 */
    T create(Throwable cause);
}
