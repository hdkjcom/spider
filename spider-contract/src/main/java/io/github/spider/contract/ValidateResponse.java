package io.github.spider.contract;

import io.github.spider.core.annotation.*;

/**
 * Annotation to mark methods for contract validation.
 * The ContractInterceptor can check response structure, required fields, etc.
 */
@java.lang.annotation.Documented
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
public @interface ValidateResponse {

    /** Expected status code, or -1 for any 2xx. */
    int expectedStatus() default -1;

    /** Whether the response body must not be empty. */
    boolean requireBody() default false;

    /** Comma-separated required JSON field paths (reserved for JSON Schema integration). */
    String[] requiredFields() default {};
}
