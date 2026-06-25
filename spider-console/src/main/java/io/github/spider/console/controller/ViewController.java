package io.github.spider.console.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@ConditionalOnClass(name = "org.thymeleaf.spring5.SpringTemplateEngine")
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "console";
    }
}
