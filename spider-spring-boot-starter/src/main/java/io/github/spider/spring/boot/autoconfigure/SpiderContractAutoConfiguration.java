package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.contract.ContractInterceptor;
import io.github.spider.core.interceptor.SpiderInterceptor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spider 契约校验自动配置类，在 classpath 中包含 ContractInterceptor 时自动激活，
 * 对请求/响应进行契约校验（仍在早期开发阶段）。
 */
@Configuration
@ConditionalOnClass(ContractInterceptor.class)
@AutoConfigureAfter(SpiderAutoConfiguration.class)
public class SpiderContractAutoConfiguration {

    /**
     * 创建契约校验拦截器。
     *
     * @return ContractInterceptor 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public SpiderInterceptor contractInterceptor() {
        return new ContractInterceptor(null);
    }
}
