package com.laoqi.assistant.controller.v3;

import com.laoqi.assistant.entity.KnowledgeBaseEntity;
import com.laoqi.assistant.service.KnowledgeBaseService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
//@RequestMapping("/v3")
public class PlannerController {

    private final KnowledgeBaseService kbService;

    public PlannerController(KnowledgeBaseService kbService) {
        this.kbService = kbService;
    }

    @GetMapping("/planner")
    public String plannerPage(@RequestParam(required = false) Long kbId, Map<String, Object> model) {
        model.put("currentNav", "planner");
        List<KnowledgeBaseEntity> kbList = kbService.getAll();
        model.put("kbList", kbList);
        
        if (!kbList.isEmpty()) {
            model.put("defaultKbId", kbList.get(0).getId());
        }

        if (kbId != null) {
            KnowledgeBaseEntity kb = kbService.getById(kbId);
            if (kb != null) {
                model.put("selectedKb", kb);
            }
        }

        return "3.0/planner";
    }
}