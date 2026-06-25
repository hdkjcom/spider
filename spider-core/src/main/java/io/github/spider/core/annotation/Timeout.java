package io.github.spider.core.annotation;

import java.lang.annotation.*;

/** Configures the timeout for a specific method (in milliseconds). */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Timeout {

    /** Timeout in milliseconds. */
    int value();
}
