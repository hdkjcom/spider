package io.github.spider.core.metadata;

/**
 * Parses method annotations and parameter annotations into MethodMetadata.
 */
public interface MethodMetadataParser {

    /**
     * Parse annotations on a method and its parameters, building a MethodMetadata.
     *
     * @param method the interface method to parse
     * @return parsed metadata, or null if the method is not a Spider-annotated method
     */
    MethodMetadata parse(java.lang.reflect.Method method);
}
