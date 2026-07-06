package io.github.spider.console.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 渲染控制台首页。版本号优先取 build-info.properties（由 spring-boot-maven-plugin 的
 * build-info goal 生成），未生成时回退到 {@code spider.console.version}（默认 1.0.1）。
 */
@Controller
public class ViewController {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    /** 未生成 build-info.properties 时的回退版本，可在 application.yml 覆盖。 */
    @Value("${spider.console.version:1.0.1}")
    private String fallbackVersion;

    @GetMapping("/spider")
    public String index(Model model) {
        model.addAttribute("version",
                buildProperties != null ? buildProperties.getVersion() : fallbackVersion);
        return "console";
    }
}
