package cn.qihang.ai.assistant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ActivityController {

    @GetMapping("/activity")
    public String activityPage(Model model) {
        model.addAttribute("currentNav", "activity");
        return "3.0/activity";
    }
}
