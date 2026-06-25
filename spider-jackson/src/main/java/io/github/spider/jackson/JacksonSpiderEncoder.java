package io.github.spider.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.spider.core.codec.SpiderEncoder;

/**
 * Jackson-based SpiderEncoder implementation.
 * Serializes Java objects to JSON bytes.
 */
public class JacksonSpiderEncoder implements SpiderEncoder {

    private final ObjectMapper objectMapper;

    public JacksonSpiderEncoder() {
        this(new ObjectMapper());
    }

    public JacksonSpiderEncoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] encode(Object object) throws Exception {
        if (object == null) {
            return new byte[0];
        }
        return objectMapper.writeValueAsBytes(object);
    }
}
