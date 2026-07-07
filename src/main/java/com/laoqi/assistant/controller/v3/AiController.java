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
public class AiController {

    private final KnowledgeBaseService kbService;
    private final NoteIndexService noteIndexService;
    private final MessageDbService messageDbService;

    public AiController(KnowledgeBaseService kbService,
                        NoteIndexService noteIndexService,
                        MessageDbService messageDbService) {
        this.kbService = kbService;
        this.noteIndexService = noteIndexService;
        this.messageDbService = messageDbService;
    }

    @GetMapping("/ai")
    public String aiPage(@RequestParam(required = false) Long kbId, Map<String, Object> model) {
        model.put("currentNav", "ai");
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
                try {
                    var stats = noteIndexService.getIndexStats(kb.getId());
                    model.put("fileCount", stats.fileCount());
                    model.put("indexCount", stats.chunkCount());
                } catch (Exception e) {
                    model.put("fileCount", 0);
                    model.put("indexCount", 0);
                }
                try {
                    model.put("totalMessages", messageDbService.countByKb(kbId));
                } catch (Exception e) {
                    model.put("totalMessages", 0);
                }
            }
        }

        return "3.0/ai";
    }
}