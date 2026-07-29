package cn.qihang.ai.assistant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class DataCenterPageController {

    @GetMapping("/data/processing")
    public String dataCenterPage(Model model) {
        model.addAttribute("pageTitle", "数据加工");
        model.addAttribute("contentFragment", "1.0/data_processing");
        return "1.0/layout";
    }
}
