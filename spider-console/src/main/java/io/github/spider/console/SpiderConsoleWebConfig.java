package io.github.spider.console;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spider Console 静态资源缓存配置。
 *
 * <p>控制台前端（spider-console.js / spider-console.css）强制不缓存，
 * 确保升级 Spider 版本后浏览器立即加载最新前端，避免旧 JS 引用已移除的 DOM 元素
 * 而抛出 "Cannot set properties of null" 等错误。
 */
@Configuration
public class SpiderConsoleWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/spider-console.js", "/spider-console.css")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noStore().mustRevalidate());
    }
}
