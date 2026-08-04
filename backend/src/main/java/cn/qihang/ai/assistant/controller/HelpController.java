package cn.qihang.ai.assistant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class HelpController {

    @GetMapping("/help")
    public String helpPage(Model model) {
        model.addAttribute("currentNav", "help");
        return "3.0/help";
    }
}