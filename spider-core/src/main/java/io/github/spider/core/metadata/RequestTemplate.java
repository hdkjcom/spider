package io.github.spider.core.metadata;

import io.github.spider.core.codec.SpiderEncoder;
import io.github.spider.core.transport.SpiderRequest;

/**
 * Builds a SpiderRequest from MethodMetadata and actual method arguments.
 */
public class RequestTemplate {

    private final SpiderEncoder encoder;

    public RequestTemplate(SpiderEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Build a SpiderRequest from metadata, arguments, and the client base URL.
     */
    public SpiderRequest build(MethodMetadata meta, Object[] args, String baseUrl) throws Exception {
        SpiderRequest request = new SpiderRequest()
                .method(meta.httpMethod())
                .url(baseUrl);

        if (meta.timeoutMillis() > 0) {
            request.timeoutMillis(meta.timeoutMillis());
        }

        // Resolve path template, query params, headers, body
        String path = meta.pathTemplate();

        for (ParamBinding binding : meta.paramBindings()) {
            Object argValue = args != null ? args[binding.index()] : null;
            switch (binding.kind()) {
                case PATH:
                    if (argValue != null) {
                        path = path.replace("{" + binding.name() + "}", String.valueOf(argValue));
                    }
                    break;
                case QUERY:
                    if (argValue != null) {
                        request.addQueryParam(binding.name(), String.valueOf(argValue));
                    }
                    break;
                case HEADER:
                    if (argValue != null) {
                        request.addHeader(binding.name(), String.valueOf(argValue));
                    }
                    break;
                case BODY:
                    if (argValue != null && encoder != null) {
                        request.body(encoder.encode(argValue));
                    }
                    break;
            }
        }

        request.path(path);
        return request;
    }
}
