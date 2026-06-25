package io.github.spider.spring.boot.autoconfigure;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enables Spider client proxies. Add this annotation to a Spring Boot application
 * or @Configuration class to auto-scan and register @SpiderClient interfaces as beans.
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

    /** Base packages to scan for @SpiderClient interfaces. Defaults to the package of the annotated class. */
    String[] basePackages() default {};

    /** Classes in the same packages or below will be scanned. */
    Class<?>[] basePackageClasses() default {};
}
