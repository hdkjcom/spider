package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.annotation.SpiderClient;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
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
 * 扫描 classpath 中标注了 @SpiderClient 的接口，并将其注册为 BeanDefinition，
 * 由 SpiderClientFactoryBean 负责代理对象的创建。
 *
 * <p>该类实现了 ImportBeanDefinitionRegistrar，在 Spring 容器启动时通过
 * {@link EnableSpiderClients} 注解触发扫描与注册流程。</p>
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

    /**
     * 扫描 basePackages 下所有标注了 @SpiderClient 的接口，并为每个接口
     * 注册一个由 SpiderClientFactoryBean 支持的 BeanDefinition。
     */
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

        builder.addConstructorArgValue(clientInterface);
        builder.addConstructorArgReference("spiderClientFactory");

        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        beanDefinition.setRole(BeanDefinition.ROLE_APPLICATION);

        String beanName = StringUtils.uncapitalize(clientName) + "SpiderClient";
        registry.registerBeanDefinition(beanName, beanDefinition);
        // 注册接口全限定名作为别名，支持按类型注入
        registry.registerAlias(beanName, clientInterface.getName());
    }

    private ClassPathScanningCandidateComponentProvider getScanner() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isIndependent();
            }
        };
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
