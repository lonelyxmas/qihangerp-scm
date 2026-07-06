package com.laoqi.assistant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class ToolsController {

    @GetMapping("/tools")
    public String toolsPage(Model model) {
        model.addAttribute("pageTitle", "工具箱");
        model.addAttribute("contentFragment", "1.0/tools");
        return "1.0/layout";
    }
}
