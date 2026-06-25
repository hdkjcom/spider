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
 * Spider 的 OpenTelemetry 链路追踪拦截器。
 * 为每次远程调用创建一个 CLIENT 类型的 Span，并注入 W3C trace-context 请求头。
 */
public class TracingInterceptor implements SpiderInterceptor {

    private static final TextMapSetter<SpiderRequest> HEADER_SETTER = (req, key, value) -> req.addHeader(key, value);
    private static final ThreadLocal<SpanContext> CURRENT_SPAN = new ThreadLocal<>();

    private final Tracer tracer;

    /**
     * 使用指定的 Tracer 构造 TracingInterceptor。
     *
     * @param tracer OpenTelemetry Tracer 实例
     */
    public TracingInterceptor(Tracer tracer) { this.tracer = tracer; }

    /**
     * 使用默认的 Spider Tracer 构造 TracingInterceptor。
     */
    public TracingInterceptor() { this(GlobalOpenTelemetry.getTracer("spider")); }

    /**
     * 在请求发送前创建 Span 并注入 W3C trace-context 请求头。
     *
     * @param request 待发送的请求
     * @return 注入了追踪信息后的请求
     */
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

    /**
     * 在收到响应后记录状态码并结束 Span。
     *
     * @param response 远程服务的响应
     * @return 原响应对象
     */
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

    /**
     * 在请求发生异常时记录异常信息到 Span 并结束 Span。
     *
     * @param request 发生异常的请求
     * @param ex      抛出的异常
     * @return false 表示继续抛出异常
     */
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
