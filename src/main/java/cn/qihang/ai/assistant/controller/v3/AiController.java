package cn.qihang.ai.assistant.controller.v3;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class AiController {

    @GetMapping("/insights")
    public String insightsPage() {
        return "redirect:/";
    }
}
