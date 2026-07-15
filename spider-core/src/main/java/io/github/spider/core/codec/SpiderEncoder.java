package io.github.spider.core.codec;

/**
 * 请求体编码 SPI（例如 Java 对象 → JSON 字节数组）。
 */
public interface SpiderEncoder {

    /** 编码器产出的默认媒体类型；transport 在 request 未声明 content-type 时用作兜底。 */
    String DEFAULT_CONTENT_TYPE = "application/json; charset=utf-8";

    /**
     * 将对象编码为字节数组。
     *
     * @param object 待编码的对象
     * @return 编码后的字节数组
     * @throws Exception 编码失败时抛出
     */
    byte[] encode(Object object) throws Exception;

    /**
     * 返回本编码器产出的媒体类型。
     *
     * <p>默认实现返回 {@link #DEFAULT_CONTENT_TYPE}。自定义编码器（如 XML/protobuf）应覆盖本方法
     * 返回实际产出的媒体类型；返回 null 表示不声明，由 transport 兜底为 {@link #DEFAULT_CONTENT_TYPE}。
     *
     * @return 编码器产出的媒体类型，或 null 表示不声明
     */
    default String contentType() { return DEFAULT_CONTENT_TYPE; }
}
