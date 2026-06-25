package io.github.spider.core.annotation;

import java.lang.annotation.*;

/**
 * Marks a method as a streaming (server-sent) gRPC call.
 * The return type must be java.util.Iterator or java.util.stream.Stream.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SpiderStream {
    /** The gRPC method name on the service. */
    String value();
}
