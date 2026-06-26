package io.github.spider.core.client;

import io.github.spider.core.exception.SpiderConfigurationException;
import io.github.spider.core.invocation.SpiderFilterChain;
import io.github.spider.core.invocation.SpiderInvocationContext;
import io.github.spider.core.metadata.MethodMetadata;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * JDK 动态代理 InvocationHandler。
 *
 * <p>将方法调用委托给 {@link SpiderFilterChain}，后者通过有序的 filter 序列执行完整的请求生命周期。
 * Object 基础方法（toString、hashCode、equals）被短路处理，不进入远程调用管道。
 *
 * <p>与旧版本不同，此类不再直接包含重试、熔断、降级、指标等治理逻辑 ——
 * 这些已提取到独立的 {@link io.github.spider.core.invocation.SpiderInvocationFilter} 实现中。
 */
public class SpiderInvocationHandler implements InvocationHandler {

    private final String clientName;
    private final String baseUrl;
    private final Map<Method, MethodMetadata> metadataCache;
    private final SpiderFilterChain chainTemplate;

    /**
     * @param clientName    逻辑服务名称
     * @param baseUrl       注解或 builder 中配置的基地址（可为空）
     * @param metadataCache 预解析的方法元数据
     * @param chainTemplate 每次调用被复制的 filter 链模板
     */
    public SpiderInvocationHandler(String clientName,
                                   String baseUrl,
                                   Map<Method, MethodMetadata> metadataCache,
                                   SpiderFilterChain chainTemplate) {
        this.clientName = clientName;
        this.baseUrl = baseUrl;
        this.metadataCache = metadataCache;
        this.chainTemplate = chainTemplate;
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

        // 每次调用创建新的上下文和链实例（链内部有游标状态，不可复用）
        SpiderInvocationContext ctx = new SpiderInvocationContext(clientName, method, args, meta, baseUrl);
        SpiderFilterChain chain = new SpiderFilterChain(chainTemplate.filters());

        return chain.next(ctx);
    }
}
