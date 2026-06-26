package io.github.spider.console.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/spider")
    public String index() {
        return "console";
    }
}
