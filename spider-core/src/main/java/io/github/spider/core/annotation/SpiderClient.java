package io.github.spider.core.annotation;

import java.lang.annotation.*;

/**
 * 声明一个远程服务客户端。
 *
 * <p>被注解的接口在运行时通过 JDK 动态代理进行代理。
 * 每个代表远程调用的方法必须携带 {@link SpiderGet}、{@link SpiderPost}、
 * {@link SpiderPut} 或 {@link SpiderDelete} 之一。
 *
 * <pre>{@code
 * @SpiderClient(name = "user-service", url = "http://localhost:8081")
 * public interface UserClient {
 *     @SpiderGet("/users/{id}")
 *     @Timeout(800)
 *     @Retry(maxAttempts = 3)
 *     UserDTO getUser(@Path("id") Long id);
 * }
 * }</pre>
 *
 * <p>在 Spring Boot 应用中，使用 {@code @EnableSpiderClients} 进行
 * 自动扫描和 Bean 注册，而不是直接调用 {@code SpiderClientFactory}。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpiderClient {

    /** 逻辑服务名称，用于指标标签和服务发现查找。 */
    String name();

    /** 远程服务的基地址，例如 {@code http://localhost:8081}。 */
    String url();

    /**
     * 实现此接口的降级类。
     * 在所有重试耗尽后调用。
     */
    Class<?> fallback() default Void.class;

    /**
     * 实现 {@code FallbackFactory<T>} 的降级工厂。
     * 优先级高于 {@link #fallback()}。
     * 工厂接收失败原因以供检查。
     */
    Class<?> fallbackFactory() default Void.class;

    /** 预留用于未来扩展。 */
    Class<?> configuration() default Void.class;
}
