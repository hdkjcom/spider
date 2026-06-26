package io.github.spider.core.metadata;

import io.github.spider.core.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * MethodMetadataParser 的默认实现。
 * 从接口方法中读取 Spider 注解并构建 MethodMetadata。
 *
 * <p>配置优先级：方法注解 > 接口注解 > Builder 默认值 > 框架默认值。
 */
public class DefaultMethodMetadataParser implements MethodMetadataParser {

    @Override
    public MethodMetadata parse(Method method) {
        return parse(method, null, -1, 1, 100);
    }

    /**
     * 解析方法元数据，支持接口级注解回退和 Builder 默认值。
     *
     * @param method                  方法
     * @param declaringClass          声明该方法的接口（用于接口级注解回退）
     * @param defaultTimeout          Builder 默认超时（毫秒），-1 表示未设置
     * @param defaultMaxAttempts      Builder 默认最大重试次数
     * @param defaultBackoffMillis    Builder 默认退避间隔（毫秒）
     * @return 方法元数据，如果方法没有 HTTP 方法注解则返回 null
     */
    public MethodMetadata parse(Method method, Class<?> declaringClass,
                                 int defaultTimeout, int defaultMaxAttempts, long defaultBackoffMillis) {
        // 检查 HTTP 方法注解
        SpiderGet getAnn = method.getAnnotation(SpiderGet.class);
        SpiderPost postAnn = method.getAnnotation(SpiderPost.class);
        SpiderPut putAnn = method.getAnnotation(SpiderPut.class);
        SpiderDelete deleteAnn = method.getAnnotation(SpiderDelete.class);

        if (getAnn == null && postAnn == null && putAnn == null && deleteAnn == null) {
            return null;
        }

        MethodMetadata meta = new MethodMetadata();

        if (getAnn != null) {
            meta.httpMethod("GET").pathTemplate(getAnn.value());
        } else if (postAnn != null) {
            meta.httpMethod("POST").pathTemplate(postAnn.value());
        } else if (putAnn != null) {
            meta.httpMethod("PUT").pathTemplate(putAnn.value());
        } else {
            meta.httpMethod("DELETE").pathTemplate(deleteAnn.value());
        }

        // ── 超时：方法注解 > 接口注解 > Builder 默认值 > 框架默认值(-1=不限制) ──
        Timeout timeoutAnn = method.getAnnotation(Timeout.class);
        if (timeoutAnn == null && declaringClass != null) {
            timeoutAnn = declaringClass.getAnnotation(Timeout.class);
        }
        if (timeoutAnn != null) {
            meta.timeoutMillis(timeoutAnn.value());
        } else if (defaultTimeout > 0) {
            meta.timeoutMillis(defaultTimeout);
        }

        // ── 重试：方法注解 > 接口注解 > Builder 默认值 > 框架默认值 ──
        Retry retryAnn = method.getAnnotation(Retry.class);
        if (retryAnn == null && declaringClass != null) {
            retryAnn = declaringClass.getAnnotation(Retry.class);
        }
        if (retryAnn != null) {
            meta.maxAttempts(retryAnn.maxAttempts())
                .backoffMillis(retryAnn.backoffMillis())
                .backoffStrategy(retryAnn.backoffStrategy().name())
                .maxBackoffMillis(retryAnn.maxBackoffMillis());
            for (Class<? extends Throwable> ex : retryAnn.retryOn()) {
                meta.addRetryOn(ex);
            }
            for (int status : retryAnn.ignoreStatus()) {
                meta.addIgnoreStatus(status);
            }
        } else {
            meta.maxAttempts(defaultMaxAttempts)
                .backoffMillis(defaultBackoffMillis);
        }

        // 返回类型
        meta.returnType(method.getGenericReturnType());

        // 解析参数注解
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Annotation[] paramAnns = parameters[i].getAnnotations();
            for (Annotation ann : paramAnns) {
                if (ann instanceof Path) {
                    meta.addParamBinding(new ParamBinding(ParamBinding.Kind.PATH, ((Path) ann).value(), i));
                } else if (ann instanceof Query) {
                    meta.addParamBinding(new ParamBinding(ParamBinding.Kind.QUERY, ((Query) ann).value(), i));
                } else if (ann instanceof Header) {
                    meta.addParamBinding(new ParamBinding(ParamBinding.Kind.HEADER, ((Header) ann).value(), i));
                } else if (ann instanceof Body) {
                    meta.addParamBinding(new ParamBinding(ParamBinding.Kind.BODY, null, i));
                }
            }
        }

        return meta;
    }
}
