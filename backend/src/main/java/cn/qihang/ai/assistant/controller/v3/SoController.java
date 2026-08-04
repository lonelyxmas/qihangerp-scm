package cn.qihang.ai.assistant.controller.v3;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class SoController {

    @GetMapping("/so")
    public String insightsPage(Model model) {
        model.addAttribute("currentNav", "so");
        return "3.0/so";
    }
}
