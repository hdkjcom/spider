package io.github.spider.core.annotation;

import java.lang.annotation.*;

/** Binds a method parameter to an HTTP header. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Header {

    /** The header name. */
    String value();
}
