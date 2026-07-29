package cn.qihang.ai.assistant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class HelpController {

    @GetMapping("/help")
    public String helpPage(Model model) {
        model.addAttribute("pageTitle", "帮助");
        model.addAttribute("contentFragment", "1.0/help");
        return "1.0/layout";
    }
}