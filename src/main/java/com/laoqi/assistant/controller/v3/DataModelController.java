package com.laoqi.assistant.controller.v3;

import com.laoqi.assistant.entity.KnowledgeBaseEntity;
import com.laoqi.assistant.service.KnowledgeBaseService;
import com.laoqi.assistant.service.NoteIndexService;
import com.laoqi.assistant.service.db.MessageDbService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/v3")
public class DataModelController {

    private final KnowledgeBaseService kbService;
    private final NoteIndexService noteIndexService;
    private final MessageDbService messageDbService;

    public DataModelController(KnowledgeBaseService kbService,
                               NoteIndexService noteIndexService,
                               MessageDbService messageDbService) {
        this.kbService = kbService;
        this.noteIndexService = noteIndexService;
        this.messageDbService = messageDbService;
    }

    @GetMapping("/data")
    public String dataPage(@RequestParam(required = false) Long kbId, Map<String, Object> model) {
        model.put("currentNav", "data");
        List<KnowledgeBaseEntity> kbList = kbService.getAll();
        model.put("kbList", kbList);
        
        if (!kbList.isEmpty()) {
            model.put("defaultKbId", kbList.get(0).getId());
        }

        if (kbId == null && !kbList.isEmpty()) {
            kbId = kbList.get(0).getId();
        }

        if (kbId != null) {
            KnowledgeBaseEntity kb = kbService.getById(kbId);
            if (kb != null) {
                model.put("selectedKb", kb);
            }
        }

        return "3.0/data";
    }

    @GetMapping("/data/module/{moduleId}")
    public String dataModulePage(@PathVariable String moduleId, Map<String, Object> model) {
        model.put("currentNav", "data");
        model.put("moduleId", moduleId);
        List<KnowledgeBaseEntity> kbList = kbService.getAll();
        model.put("kbList", kbList);
        
        if (!kbList.isEmpty()) {
            model.put("defaultKbId", kbList.get(0).getId());
        }

        return "3.0/data-module";
    }
}