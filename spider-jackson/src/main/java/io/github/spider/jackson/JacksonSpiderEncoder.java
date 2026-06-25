package io.github.spider.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.spider.core.codec.SpiderEncoder;

/**
 * Jackson-based SpiderEncoder implementation.
 * Serializes Java objects to JSON bytes.
 */
public class JacksonSpiderEncoder implements SpiderEncoder {

    private final ObjectMapper objectMapper;

    /**
     * 使用默认配置的 {@link ObjectMapper} 创建编码器实例。
     */
    public JacksonSpiderEncoder() {
        this(new ObjectMapper());
    }

    /**
     * 使用自定义的 {@link ObjectMapper} 创建编码器实例。
     *
     * @param objectMapper 自定义的 Jackson ObjectMapper 实例
     */
    public JacksonSpiderEncoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将 Java 对象序列化为 JSON 字节数组。
     * <p>
     * 约定：
     * <ul>
     *   <li>object 为 {@code null} 时返回空字节数组（长度为 0），不抛异常</li>
     *   <li>非空对象通过 Jackson 的 {@link ObjectMapper#writeValueAsBytes(Object)} 进行序列化</li>
     * </ul>
     *
     * @param object 待序列化的 Java 对象，可为 {@code null}
     * @return JSON 字节数组，当 object 为 {@code null} 时返回空字节数组
     * @throws Exception 序列化过程中可能抛出 Jackson 相关异常（如 {@code JsonProcessingException}）
     */
    @Override
    public byte[] encode(Object object) throws Exception {
        if (object == null) {
            return new byte[0];
        }
        return objectMapper.writeValueAsBytes(object);
    }
}
