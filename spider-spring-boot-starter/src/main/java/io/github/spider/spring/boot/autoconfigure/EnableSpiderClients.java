package io.github.spider.spring.boot.autoconfigure;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 Spider 客户端代理。在 Spring Boot 启动类或 {@code @Configuration} 类上添加此注解，
 * 即可自动扫描并注册标注了 {@code @SpiderClient} 的接口为 Spring Bean。
 *
 * <pre>{@code
 * @SpringBootApplication
 * @EnableSpiderClients(basePackages = "com.example.client")
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(SpiderClientRegistrar.class)
public @interface EnableSpiderClients {

    /** 需要扫描 @SpiderClient 接口的基包路径，默认为注解所在类的包路径。 */
    String[] basePackages() default {};

    /** 指定基类，其所在包及子包将被扫描。 */
    Class<?>[] basePackageClasses() default {};
}
