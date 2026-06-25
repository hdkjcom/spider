package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.reporter.SpiderReporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Spider 控制台上报自动配置类，在配置了 {@code spider.console.url} 时激活。
 * 应用启动后就绪后自动向 Spider Console 上报服务信息。
 */
@Configuration
@ConditionalOnProperty(prefix = "spider.console", name = "url")
public class SpiderReporterAutoConfiguration {

    @Value("${spider.console.url}")
    private String consoleUrl;

    @Value("${spider.console.service-name:${spring.application.name:unknown}}")
    private String serviceName;

    /**
     * 在应用就绪后启动控制台上报，向 Spider Console 发送心跳与元数据。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startReporter() {
        SpiderReporter.start(consoleUrl, serviceName);
    }
}
