package com.laoqi.assistant.controller.v3;

import com.laoqi.assistant.entity.KnowledgeBaseEntity;
import com.laoqi.assistant.service.KnowledgeBaseService;
import com.laoqi.assistant.util.FileUtil;
import com.laoqi.assistant.util.MarkdownUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Controller
//@RequestMapping("/v3")
public class NotesController {

    private static final Logger log = LoggerFactory.getLogger(NotesController.class);

    private final KnowledgeBaseService kbService;

    public NotesController(KnowledgeBaseService kbService) {
        this.kbService = kbService;
    }

    @GetMapping("/notes")
    public String notesPage(@RequestParam(required = false) Long kbId, Map<String, Object> model) {
        model.put("currentNav", "notes");
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

        return "3.0/notes";
    }
}
