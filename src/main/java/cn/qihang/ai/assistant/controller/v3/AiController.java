package cn.qihang.ai.assistant.controller.v3;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class AiController {

    @GetMapping("/so")
    public String insightsPage(Model model) {
        model.addAttribute("currentNav", "so");
        return "3.0/insights";
    }
}
