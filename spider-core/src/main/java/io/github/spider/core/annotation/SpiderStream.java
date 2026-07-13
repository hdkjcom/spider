package io.github.spider.core.annotation;

import java.lang.annotation.*;

/**
 * 标记方法为流式（服务端推送）调用。
 *
 * <p><b>注意：此功能尚未完整实现。</b>当前 {@code @SpiderStream} 仅被解析为普通 HTTP GET 请求，
 * 不提供流式解码或 {@code Iterator}/{@code Stream} 返回类型处理。
 * 此注解保留用于未来版本，届时将支持完整的流式调用和分块传输。
 *
 * <p>已计划但未实现的功能：
 * <ul>
 *   <li>{@code Iterator<T>} 返回类型处理</li>
 *   <li>{@code java.util.stream.Stream<T>} 返回类型处理</li>
 *   <li>分块传输解码（chunked transfer decoding）</li>
 *   <li>服务端推送完整支持</li>
 * </ul>
 *
 * @deprecated 尚未完整实现，当前仅作为普通 HTTP GET 执行。保留供未来版本使用。
 */
@Deprecated
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SpiderStream {
    /** 方法名。 */
    String value();
}
