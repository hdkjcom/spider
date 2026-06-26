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
 *
 * <h3>配置优先级</h3>
 * <p>当同一配置项存在多个来源时，按以下优先级解析（高到低）：
 * <ol>
 *   <li><b>方法注解</b> — 方法上的 {@code @Timeout}、{@code @Retry} 等</li>
 *   <li><b>接口注解</b> — 接口上的 {@code @Timeout}、{@code @Retry} 等（方法未指定时生效）</li>
 *   <li><b>Spring 属性</b> — {@code application.yml} 中的 {@code spider.*} 配置</li>
 *   <li><b>Builder 默认值</b> — {@code SpiderClientFactory.builder()} 指定的值</li>
 *   <li><b>框架默认值</b> — 各注解属性自身的默认值</li>
 * </ol>
 *
 * <p>适用范围：超时、重试、熔断、限流、URL、服务名、降级、编解码、传输、指标、追踪、服务发现。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpiderClient {

    /** 逻辑服务名称，用于指标标签和服务发现查找。 */
    String name();

    /** 远程服务的基地址，例如 {@code http://localhost:8081}。 */
    String url() default "";

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
