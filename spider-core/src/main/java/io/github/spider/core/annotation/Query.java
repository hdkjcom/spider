package io.github.spider.core.annotation;

import java.lang.annotation.*;

/** Binds a method parameter to a URL query parameter. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Query {

    /** The query parameter name. */
    String value();
}
