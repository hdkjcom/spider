package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.contract.ContractInterceptor;
import io.github.spider.core.interceptor.SpiderInterceptor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ContractInterceptor.class)
@AutoConfigureAfter(SpiderAutoConfiguration.class)
public class SpiderContractAutoConfiguration {

    @Bean
    public SpiderInterceptor contractInterceptor() {
        return new ContractInterceptor(null);
    }
}
