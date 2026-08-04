package cn.qihang.ai.assistant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AiGuideController {

    @GetMapping("/ai-guide")
    public String aiGuidePage(@RequestParam(required = false) Long kbId, Model model) {
        model.addAttribute("pageTitle", "AI 指南");
        model.addAttribute("contentFragment", "1.0/kb_ai_guide");
        return "1.0/layout";
    }
}
