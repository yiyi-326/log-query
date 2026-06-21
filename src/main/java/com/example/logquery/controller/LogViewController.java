package com.example.logquery.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LogViewController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/query")
    public String queryPage() {
        return "query";
    }

    @GetMapping("/import")
    public String importPage() {
        return "import";
    }

    @GetMapping("/apps")
    public String appsPage() {
        return "apps";
    }

    @GetMapping("/alerts")
    public String alertsPage() {
        return "alert-rules";
    }

    @GetMapping("/alert-history")
    public String alertHistoryPage() {
        return "alert-history";
    }
}

