package io.github.spider.core.codec;

import java.lang.reflect.Type;

/**
 * SPI for decoding response bodies (e.g. JSON bytes → Java object).
 */
public interface SpiderDecoder {

    /**
     * Decode bytes to an object of the given type.
     *
     * @param bodyBytes the raw response body
     * @param returnType the target Java type (supports generics via ParameterizedType)
     * @return the decoded object
     * @throws Exception on decoding failure
     */
    Object decode(byte[] bodyBytes, Type returnType) throws Exception;
}
