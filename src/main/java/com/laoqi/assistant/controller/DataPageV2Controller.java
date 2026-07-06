package com.laoqi.assistant.controller;

import com.laoqi.assistant.service.KnowledgeBaseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DataPageV2Controller {

    private final KnowledgeBaseService kbService;

    public DataPageV2Controller(KnowledgeBaseService kbService) {
        this.kbService = kbService;
    }

    @GetMapping("/data")
    public String dataPage(@RequestParam(required = false) Long kbId, Model model) {
        if (kbId != null) {
            model.addAttribute("kbId", kbId);
            var kb = kbService.getById(kbId);
            if (kb != null) model.addAttribute("currentKb", kb);
        }
        model.addAttribute("pageTitle", "数据中心");
        model.addAttribute("contentFragment", "2.0/data");
        return "2.0/layout";
    }

    @GetMapping("/data/module/{moduleId}")
    public String modulePage(@PathVariable String moduleId, @RequestParam(required = false) Long kbId, Model model) {
        model.addAttribute("moduleId", moduleId);
        if (kbId != null) {
            model.addAttribute("kbId", kbId);
            var kb = kbService.getById(kbId);
            if (kb != null) model.addAttribute("currentKb", kb);
        }
        model.addAttribute("pageTitle", "数据模块");
        model.addAttribute("contentFragment", "2.0/data-module");
        return "2.0/layout";
    }
}
