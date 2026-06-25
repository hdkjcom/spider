package io.github.spider.core.annotation;

import java.lang.annotation.*;

/**
 * Declares a remote service client.
 *
 * <p>The annotated interface is proxied via JDK dynamic proxy at runtime.
 * Each method representing a remote call must carry one of
 * {@link SpiderGet}, {@link SpiderPost}, {@link SpiderPut}, or {@link SpiderDelete}.
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
 * <p>In a Spring Boot application, use {@code @EnableSpiderClients} for
 * automatic scanning and bean registration instead of calling
 * {@code SpiderClientFactory} directly.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpiderClient {

    /** Logical service name, used in metrics tags and service discovery lookups. */
    String name();

    /** Base URL of the remote service, e.g. {@code http://localhost:8081}. */
    String url();

    /**
     * Fallback class that implements this interface.
     * Invoked after all retries are exhausted.
     */
    Class<?> fallback() default Void.class;

    /**
     * Fallback factory implementing {@code FallbackFactory<T>}.
     * Takes precedence over {@link #fallback()}.
     * The factory receives the failure cause for inspection.
     */
    Class<?> fallbackFactory() default Void.class;

    /** Reserved for future use. */
    Class<?> configuration() default Void.class;
}
