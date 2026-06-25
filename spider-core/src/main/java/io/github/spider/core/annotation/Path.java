package io.github.spider.core.annotation;

import java.lang.annotation.*;

/** Binds a method parameter to a URI path variable (e.g. /users/{id}). */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Path {

    /** The path variable name. */
    String value();
}
