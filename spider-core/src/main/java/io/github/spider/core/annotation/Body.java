package io.github.spider.core.annotation;

import java.lang.annotation.*;

/** Marks a method parameter as the HTTP request body. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Body {
}
