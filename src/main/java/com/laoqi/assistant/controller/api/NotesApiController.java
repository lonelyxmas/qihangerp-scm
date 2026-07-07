package com.laoqi.assistant.controller.api;

import com.laoqi.assistant.entity.KnowledgeBaseEntity;
import com.laoqi.assistant.service.KnowledgeBaseService;
import com.laoqi.assistant.util.FileUtil;
import com.laoqi.assistant.util.MarkdownUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/v3")
public class NotesApiController {

    private static final Logger log = LoggerFactory.getLogger(NotesApiController.class);

    private final KnowledgeBaseService kbService;

    public NotesApiController(KnowledgeBaseService kbService) {
        this.kbService = kbService;
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