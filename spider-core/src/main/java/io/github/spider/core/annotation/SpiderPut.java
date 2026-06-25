package io.github.spider.core.annotation;

import java.lang.annotation.*;

/** Declares an HTTP PUT request. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SpiderPut {
    String value();
    String[] headers() default {};
}
