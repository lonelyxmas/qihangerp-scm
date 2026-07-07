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
@RequestMapping("/v3")
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

    @ResponseBody
    @GetMapping("/api/notes/tree")
    public Map<String, Object> getNotesTree(@RequestParam Long kbId) {
        KnowledgeBaseEntity kb = kbService.getById(kbId);
        if (kb == null) return Map.of("ok", false, "error", "笔记库不存在");

        Path base = Paths.get(kb.getNotesDir());
        if (kb.getNotesDir() == null || kb.getNotesDir().isBlank()) {
            return Map.of("ok", true, "tree", Map.of());
        }

        Map<String, Object> tree = buildFileTree(base, base);
        return Map.of("ok", true, "tree", tree);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildFileTree(Path root, Path current) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> dirs = new ArrayList<>();
        List<Map<String, String>> files = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
            List<Path> entries = new ArrayList<>();
            stream.forEach(entries::add);

            entries.sort((a, b) -> {
                boolean aIsDir = Files.isDirectory(a);
                boolean bIsDir = Files.isDirectory(b);
                if (aIsDir != bIsDir) return aIsDir ? -1 : 1;
                return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
            });

            for (Path entry : entries) {
                String name = entry.getFileName().toString();

                if (name.startsWith(".") || name.equals("__pycache__")) continue;

                String relativePath = root.relativize(entry).toString().replace("\\", "/");

                if (Files.isDirectory(entry)) {
                    if (name.equals("AI") || name.equals(".git") || name.equals(".obsidian")) continue;

                    Map<String, Object> dir = new LinkedHashMap<>();
                    dir.put("name", name);
                    dir.put("path", relativePath);
                    dir.put("children", buildFileTree(root, entry));
                    dirs.add(dir);
                } else if (name.endsWith(".md")) {
                    Map<String, String> file = new LinkedHashMap<>();
                    file.put("name", name);
                    file.put("path", relativePath);
                    files.add(file);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to list directory: {}", current);
        }

        result.put("dirs", dirs);
        result.put("files", files);
        return result;
    }

    @ResponseBody
    @GetMapping("/api/notes/read")
    public Map<String, Object> readNote(@RequestParam Long kbId, @RequestParam String path) {
        KnowledgeBaseEntity kb = kbService.getById(kbId);
        if (kb == null) return Map.of("ok", false, "error", "笔记库不存在");

        Path base = Paths.get(kb.getNotesDir());
        Path file = safeResolve(base, path);

        if (!Files.isRegularFile(file)) {
            return Map.of("ok", false, "error", "文件不存在");
        }

        try {
            String content = FileUtil.readText(file);
            content = MarkdownUtil.stripFrontmatter(content);
            return Map.of("ok", true, "content", content);
        } catch (Exception e) {
            return Map.of("ok", false, "error", "读取失败");
        }
    }

    private Path safeResolve(Path base, String rel) {
        Path normalized = base.normalize();
        Path resolved = normalized.resolve(rel != null ? rel : "").normalize();
        if (!resolved.startsWith(normalized)) return normalized;
        return resolved;
    }
}