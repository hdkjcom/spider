package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.reporter.SpiderReporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
@ConditionalOnProperty(prefix = "spider.console", name = "url")
public class SpiderReporterAutoConfiguration {

    @Value("${spider.console.url}")
    private String consoleUrl;

    @Value("${spider.console.service-name:${spring.application.name:unknown}}")
    private String serviceName;

    @EventListener(ApplicationReadyEvent.class)
    public void startReporter() {
        SpiderReporter.start(consoleUrl, serviceName);
    }
}
