package com.laoqi.assistant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class DataImportController {

    @GetMapping("/data/import")
    public String importPage(Model model) {
        model.addAttribute("pageTitle", "数据导入");
        model.addAttribute("contentFragment", "1.0/data_import");
        return "1.0/layout";
    }
}
