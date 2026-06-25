package io.github.spider.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.spider.core.codec.SpiderDecoder;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * Jackson-based SpiderDecoder implementation.
 * Deserializes JSON bytes to Java objects with generic type support.
 */
public class JacksonSpiderDecoder implements SpiderDecoder {

    private final ObjectMapper objectMapper;

    public JacksonSpiderDecoder() {
        this(new ObjectMapper());
    }

    public JacksonSpiderDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object decode(byte[] bodyBytes, Type returnType) throws Exception {
        if (bodyBytes == null || bodyBytes.length == 0) {
            return null;
        }
        // String return type: return raw body, don't parse JSON
        if (returnType == String.class) {
            return new String(bodyBytes, StandardCharsets.UTF_8);
        }
        JavaType javaType = objectMapper.getTypeFactory().constructType(returnType);
        return objectMapper.readValue(bodyBytes, javaType);
    }
}
