package io.github.spider.core.client;

import io.github.spider.core.exception.SpiderConfigurationException;
import io.github.spider.core.invocation.SpiderFilterChain;
import io.github.spider.core.invocation.SpiderInvocationContext;
import io.github.spider.core.metadata.MethodMetadata;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * JDK 动态代理 InvocationHandler。
 *
 * <p>将方法调用委托给 {@link SpiderFilterChain}，后者通过有序的 filter 序列执行完整的请求生命周期。
 * Object 基础方法（toString、hashCode、equals）被短路处理，不进入远程调用管道。
 *
 * <p>支持异步调用：当方法返回类型为 {@link CompletableFuture} 时，调用被提交到异步线程池执行，
 * 同步管道逻辑不变。这允许调用方以非阻塞方式集成（如 Spring MVC 返回 CompletableFuture）。
 */
public class SpiderInvocationHandler implements InvocationHandler {

    private final String clientName;
    private final String baseUrl;
    private final Map<Method, MethodMetadata> metadataCache;
    private final SpiderFilterChain chainTemplate;
    private final ExecutorService asyncExecutor;

    public SpiderInvocationHandler(String clientName,
                                   String baseUrl,
                                   Map<Method, MethodMetadata> metadataCache,
                                   SpiderFilterChain chainTemplate) {
        this(clientName, baseUrl, metadataCache, chainTemplate, ForkJoinPool.commonPool());
    }

    public SpiderInvocationHandler(String clientName,
                                   String baseUrl,
                                   Map<Method, MethodMetadata> metadataCache,
                                   SpiderFilterChain chainTemplate,
                                   ExecutorService asyncExecutor) {
        this.clientName = clientName;
        this.baseUrl = baseUrl;
        this.metadataCache = metadataCache;
        this.chainTemplate = chainTemplate;
        this.asyncExecutor = asyncExecutor != null ? asyncExecutor : ForkJoinPool.commonPool();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Object 基础方法短路处理
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        MethodMetadata meta = metadataCache.get(method);
        if (meta == null) {
            throw new SpiderConfigurationException("No Spider metadata found for method: " + method.getName()
                    + ". Is @SpiderGet or @SpiderPost missing?");
        }

        // 异步调用：返回类型为 CompletableFuture 时，提交到线程池
        if (isCompletableFuture(method.getGenericReturnType())) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return doInvoke(method, args, meta);
                } catch (Throwable t) {
                    if (t instanceof RuntimeException) throw (RuntimeException) t;
                    if (t instanceof Error) throw (Error) t;
                    throw new RuntimeException(t);
                }
            }, asyncExecutor);
        }

        // 同步调用
        return doInvoke(method, args, meta);
    }

    private Object doInvoke(Method method, Object[] args, MethodMetadata meta) throws Throwable {
        SpiderInvocationContext ctx = new SpiderInvocationContext(clientName, method, args, meta, baseUrl);
        SpiderFilterChain chain = new SpiderFilterChain(chainTemplate.filters());
        return chain.next(ctx);
    }

    /** 判断返回类型是否为 CompletableFuture。 */
    private static boolean isCompletableFuture(Type returnType) {
        if (returnType == CompletableFuture.class) return true;
        if (returnType instanceof ParameterizedType) {
            return ((ParameterizedType) returnType).getRawType() == CompletableFuture.class;
        }
        return returnType instanceof Class && CompletableFuture.class.isAssignableFrom((Class<?>) returnType);
    }
}
