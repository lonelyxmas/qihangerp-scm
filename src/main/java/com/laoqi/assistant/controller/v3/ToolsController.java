package com.laoqi.assistant.controller.v3;

import com.laoqi.assistant.entity.KnowledgeBaseEntity;
import com.laoqi.assistant.service.KnowledgeBaseService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
//@RequestMapping("/v3")
public class ToolsController {

    private final KnowledgeBaseService kbService;

    public ToolsController(KnowledgeBaseService kbService) {
        this.kbService = kbService;
    }

    @GetMapping("/tools")
    public String toolsPage(Map<String, Object> model) {
        model.put("currentNav", "tools");
        List<KnowledgeBaseEntity> kbList = kbService.getAll();
        model.put("kbList", kbList);
        
        if (!kbList.isEmpty()) {
            model.put("defaultKbId", kbList.get(0).getId());
        }

        return "3.0/tools";
    }
}