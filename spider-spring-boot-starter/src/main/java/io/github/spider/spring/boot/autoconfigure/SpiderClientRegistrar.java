package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.annotation.SpiderClient;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Scans the classpath for @SpiderClient interfaces and registers them as bean definitions
 * backed by SpiderClientFactoryBean.
 */
public class SpiderClientRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private ResourceLoader resourceLoader;
    private Environment environment;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Set<String> basePackages = getBasePackages(importingClassMetadata);
        ClassPathScanningCandidateComponentProvider scanner = getScanner();

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                if (candidate instanceof AnnotatedBeanDefinition) {
                    AnnotationMetadata annMeta = ((AnnotatedBeanDefinition) candidate).getMetadata();
                    Map<String, Object> attrs = annMeta.getAnnotationAttributes(SpiderClient.class.getName());
                    if (attrs == null) continue;

                    String className = annMeta.getClassName();
                    Class<?> clientInterface;
                    try {
                        clientInterface = ClassUtils.forName(className, Thread.currentThread().getContextClassLoader());
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("Failed to load @SpiderClient interface: " + className, e);
                    }

                    registerClientBean(registry, clientInterface, (String) attrs.get("name"));
                }
            }
        }
    }

    private void registerClientBean(BeanDefinitionRegistry registry, Class<?> clientInterface, String clientName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(SpiderClientFactoryBean.class);

        // Pass the client interface class as a constructor argument
        builder.addConstructorArgValue(clientInterface);
        // Reference to the shared SpiderClientFactory bean
        builder.addConstructorArgReference("spiderClientFactory");

        builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        beanDefinition.setPrimary(true);

        String beanName = StringUtils.uncapitalize(clientName) + "SpiderClient";
        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, beanName, new String[]{clientInterface.getName()});
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }

    private ClassPathScanningCandidateComponentProvider getScanner() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, environment);
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(SpiderClient.class));
        return scanner;
    }

    private Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attrs = importingClassMetadata
                .getAnnotationAttributes(EnableSpiderClients.class.getName());
        if (attrs == null) {
            return new HashSet<>();
        }

        Set<String> basePackages = new HashSet<>();

        for (String pkg : (String[]) attrs.get("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }

        for (Class<?> clazz : (Class<?>[]) attrs.get("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }

        return basePackages;
    }
}
