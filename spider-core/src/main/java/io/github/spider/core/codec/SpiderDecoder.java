package io.github.spider.core.codec;

import java.lang.reflect.Type;

/**
 * 响应体解码 SPI（例如 JSON 字节数组 → Java 对象）。
 */
public interface SpiderDecoder {

    /**
     * 将字节数组解码为指定类型的对象。
     *
     * @param bodyBytes 原始响应体字节数组
     * @param returnType 目标 Java 类型（通过 ParameterizedType 支持泛型）
     * @return 解码后的对象
     * @throws Exception 解码失败时抛出
     */
    Object decode(byte[] bodyBytes, Type returnType) throws Exception;
}
