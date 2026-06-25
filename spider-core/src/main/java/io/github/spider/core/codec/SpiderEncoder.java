package io.github.spider.core.codec;

import java.lang.reflect.Type;

/**
 * SPI for encoding request bodies (e.g. Java object → JSON bytes).
 */
public interface SpiderEncoder {

    /**
     * Encode an object to bytes.
     *
     * @param object the object to encode
     * @return encoded bytes
     * @throws Exception on encoding failure
     */
    byte[] encode(Object object) throws Exception;
}
