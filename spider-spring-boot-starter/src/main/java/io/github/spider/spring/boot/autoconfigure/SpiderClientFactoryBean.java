package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.client.SpiderClientFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * Spring FactoryBean，用于创建 Spider 客户端代理对象。
 * 每个 @SpiderClient 接口对应一个独立的 FactoryBean 实例。
 */
public class SpiderClientFactoryBean implements FactoryBean<Object> {

    private final Class<?> clientInterface;
    private final SpiderClientFactory factory;

    /**
     * 构造 Spider 客户端 FactoryBean。
     *
     * @param clientInterface 标注了 @SpiderClient 的接口类型
     * @param factory         SpiderClientFactory 实例
     */
    public SpiderClientFactoryBean(Class<?> clientInterface, SpiderClientFactory factory) {
        this.clientInterface = clientInterface;
        this.factory = factory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getObject() {
        return factory.create(clientInterface);
    }

    @Override
    public Class<?> getObjectType() {
        return clientInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
