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

    /**
     * 使用默认配置的 {@link ObjectMapper} 创建解码器实例。
     */
    public JacksonSpiderDecoder() {
        this(new ObjectMapper());
    }

    /**
     * 使用自定义的 {@link ObjectMapper} 创建解码器实例。
     *
     * @param objectMapper 自定义的 Jackson ObjectMapper 实例
     */
    public JacksonSpiderDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将 HTTP 响应体字节数组反序列化为指定类型的 Java 对象。
     * <p>
     * 约定：
     * <ul>
     *   <li>bodyBytes 为 {@code null} 或空数组时返回 {@code null}</li>
     *   <li>returnType 为 {@link String String.class} 时，直接返回原始 UTF-8 字符串，不做 JSON 解析</li>
     *   <li>其他类型通过 Jackson 的 {@link ObjectMapper#readValue(byte[], JavaType)} 进行反序列化，
     *       支持泛型类型（如 {@code List<User>}、{@code Map<String, Object>} 等）</li>
     * </ul>
     *
     * @param bodyBytes  HTTP 响应体字节数组，不可为 {@code null}（调用方应保证）
     * @param returnType 目标反序列化类型，通常是接口方法的返回值类型或泛型类型
     * @return 反序列化后的 Java 对象，当 bodyBytes 为空时返回 {@code null}
     * @throws Exception 反序列化过程中可能抛出 Jackson 相关异常（如 {@code JsonProcessingException}）
     */
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
