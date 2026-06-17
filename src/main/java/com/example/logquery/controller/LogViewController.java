package com.example.logquery.controller;

import com.example.logquery.service.LogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LogViewController {

    private final LogService logService;

    public LogViewController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("stats", logService.getStats());
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
}

