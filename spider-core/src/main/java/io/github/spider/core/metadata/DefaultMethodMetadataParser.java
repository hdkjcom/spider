package io.github.spider.core.metadata;

import io.github.spider.core.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * MethodMetadataParser 的默认实现。
 * 从接口方法中读取 Spider 注解并构建 MethodMetadata。
 */
public class DefaultMethodMetadataParser implements MethodMetadataParser {

    @Override
    public MethodMetadata parse(Method method) {
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

        // 超时配置
        Timeout timeoutAnn = method.getAnnotation(Timeout.class);
        if (timeoutAnn != null) {
            meta.timeoutMillis(timeoutAnn.value());
        }

        // 重试配置
        Retry retryAnn = method.getAnnotation(Retry.class);
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
