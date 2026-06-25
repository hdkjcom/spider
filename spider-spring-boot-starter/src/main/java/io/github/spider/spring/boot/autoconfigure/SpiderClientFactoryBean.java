package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.client.SpiderClientFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * Spring FactoryBean that creates Spider client proxies.
 * Each @SpiderClient interface gets its own FactoryBean.
 */
public class SpiderClientFactoryBean implements FactoryBean<Object> {

    private final Class<?> clientInterface;
    private final SpiderClientFactory factory;

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
