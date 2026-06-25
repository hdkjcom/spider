package io.github.spider.telemetry;

import io.github.spider.core.interceptor.SpiderInterceptor;
import io.github.spider.core.transport.SpiderRequest;
import io.github.spider.core.transport.SpiderResponse;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;

/**
 * OpenTelemetry tracing interceptor for Spider.
 * Creates a CLIENT span for each remote call and injects W3C trace-context headers.
 */
public class TracingInterceptor implements SpiderInterceptor {

    private static final TextMapSetter<SpiderRequest> HEADER_SETTER = (req, key, value) -> req.addHeader(key, value);
    private static final ThreadLocal<SpanContext> CURRENT_SPAN = new ThreadLocal<>();

    private final Tracer tracer;

    public TracingInterceptor(Tracer tracer) { this.tracer = tracer; }

    public TracingInterceptor() { this(GlobalOpenTelemetry.getTracer("spider")); }

    @Override
    public SpiderRequest beforeRequest(SpiderRequest request) {
        Span span = tracer.spanBuilder(request.method() + " " + request.path())
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("http.method", request.method())
                .setAttribute("http.url", request.fullUrl())
                .startSpan();
        Scope scope = span.makeCurrent();
        CURRENT_SPAN.set(new SpanContext(span, scope));

        // Inject W3C trace-context into HTTP headers
        GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                .inject(Context.current(), request, HEADER_SETTER);
        return request;
    }

    @Override
    public SpiderResponse afterResponse(SpiderResponse response) {
        SpanContext ctx = CURRENT_SPAN.get();
        if (ctx != null) {
            if (!response.isSuccessful()) {
                ctx.span.setStatus(StatusCode.ERROR, "HTTP " + response.statusCode());
            }
            ctx.span.setAttribute("http.status_code", response.statusCode());
            ctx.span.end();
            ctx.scope.close();
            CURRENT_SPAN.remove();
        }
        return response;
    }

    @Override
    public boolean onError(SpiderRequest request, Exception ex) {
        SpanContext ctx = CURRENT_SPAN.get();
        if (ctx != null) {
            ctx.span.setStatus(StatusCode.ERROR, ex.getMessage() != null ? ex.getMessage() : "error");
            ctx.span.recordException(ex);
            ctx.span.end();
            ctx.scope.close();
            CURRENT_SPAN.remove();
        }
        return false;
    }

    private static class SpanContext {
        final Span span;
        final Scope scope;
        SpanContext(Span span, Scope scope) { this.span = span; this.scope = scope; }
    }
}
