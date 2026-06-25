package io.github.spider.admin;

import io.github.spider.core.runtime.SpiderRuntime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

@Controller
public class SpiderDashboardController {

    private final SpiderAdminService adminService = new SpiderAdminService();

    /** Dashboard home page — server-side rendered. */
    @GetMapping("/")
    public String dashboard(Model model) {
        Map<String, Object> summary = adminService.summary();
        Map<String, Map<String, Object>> clients = adminService.clients();
        Map<String, String> breakers = adminService.circuitBreakers();

        model.addAttribute("summary", summary);
        model.addAttribute("clients", clients);
        model.addAttribute("breakers", breakers);
        model.addAttribute("clientCount", clients.size());
        model.addAttribute("breakerCount", breakers.size());
        model.addAttribute("now", new Date());

        return "dashboard";
    }

    // REST endpoints still available for AJAX
    @GetMapping("/spider/dashboard")
    public String dashboardJson(Model model) {
        // Redirect HTML requests to the Thymeleaf page
        return "forward:/";
    }
}
