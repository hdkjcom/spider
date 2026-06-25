package io.github.spider.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法以进行契约验证的注解。
 * ContractInterceptor 可检查响应结构、必填字段等。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ValidateResponse {

    /** 期望的状态码，-1 表示任意 2xx。 */
    int expectedStatus() default -1;

    /** 响应体是否不能为空。 */
    boolean requireBody() default false;

    /** 逗号分隔的必填 JSON 字段路径（预留给 JSON Schema 集成）。 */
    String[] requiredFields() default {};
}
