package cn.qihang.ai.assistant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class CollectorPageController {

    @GetMapping("/data/collector")
    public String collectorPage(Model model) {
        model.addAttribute("pageTitle", "数据采集");
        model.addAttribute("contentFragment", "1.0/collector");
        return "1.0/layout";
    }
}