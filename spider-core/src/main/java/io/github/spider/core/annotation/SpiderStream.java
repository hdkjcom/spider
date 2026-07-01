package io.github.spider.core.annotation;

import java.lang.annotation.*;

/**
 * 标记方法为流式（服务端推送）调用的 gRPC 调用。
 * 返回类型必须是 java.util.Iterator 或 java.util.stream.Stream。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SpiderStream {
    /** 服务上的 gRPC 方法名。 */
    String value();
}
