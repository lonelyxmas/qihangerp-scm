package cn.qihang.ai.assistant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AutomationController {

    @GetMapping("/automation")
    public String automationPage(Model model) {
        model.addAttribute("currentNav", "automation");
        return "3.0/automation";
    }
}
