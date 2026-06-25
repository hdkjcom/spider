package io.github.spider.core.annotation;

import java.lang.annotation.*;

/** Declares an HTTP DELETE request. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SpiderDelete {
    String value();
    String[] headers() default {};
}
