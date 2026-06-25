package io.github.spider.admin;

import io.github.spider.core.runtime.SpiderRuntime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SpiderApiController {

    @GetMapping("/api/dashboard")
    public Map<String, Object> dashboard() {
        return SpiderRuntime.getInstance().fullReport();
    }
}
