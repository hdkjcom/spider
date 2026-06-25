package io.github.spider.console;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spider 独立控制台启动类。
 * 运行在独立端口，接收业务系统上报的指标数据并提供监控页面。
 */
@SpringBootApplication
public class SpiderConsoleApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpiderConsoleApplication.class, args);
    }
}
