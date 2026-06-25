package io.github.spider.core.codec;

import java.lang.reflect.Type;

/**
 * 请求体编码 SPI（例如 Java 对象 → JSON 字节数组）。
 */
public interface SpiderEncoder {

    /**
     * 将对象编码为字节数组。
     *
     * @param object 待编码的对象
     * @return 编码后的字节数组
     * @throws Exception 编码失败时抛出
     */
    byte[] encode(Object object) throws Exception;
}
