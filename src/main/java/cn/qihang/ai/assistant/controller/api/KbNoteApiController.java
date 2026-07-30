package cn.qihang.ai.assistant.controller.api;

import cn.qihang.ai.assistant.entity.KbNoteEntity;
import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.service.DocumentParserService;
import cn.qihang.ai.assistant.service.KbBaseService;
import cn.qihang.ai.assistant.service.LogService;
import cn.qihang.ai.assistant.service.db.KbNoteDbService;
import cn.qihang.ai.assistant.util.TimeUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/kb")
public class KbNoteApiController {

    private final KbBaseService kbService;
    private final KbNoteDbService kbNoteDbService;
    private final LogService logService;
    private final DocumentParserService documentParserService;

    public KbNoteApiController(KbBaseService kbService,
                               KbNoteDbService kbNoteDbService,
                               LogService logService,
                               DocumentParserService documentParserService) {
        this.kbService = kbService;
        this.kbNoteDbService = kbNoteDbService;
        this.logService = logService;
        this.documentParserService = documentParserService;
    }

    @GetMapping("/{id}/notes/tree")
    public Map<String, Object> getTree(@PathVariable Long id) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");
        try {
            List<KbNoteEntity> all = kbNoteDbService.listByKbId(id);
            Map<String, Object> tree = buildTree(all);
            return Map.of("ok", true, "tree", tree);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @GetMapping("/{id}/notes/read")
    public Map<String, Object> readNote(@PathVariable Long id, @RequestParam String path) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");
        KbNoteEntity note = kbNoteDbService.getByKbIdAndPath(id, path);
        if (note == null) return Map.of("ok", false, "error", "文件不存在");
        if (note.getIsDir() == 1) return Map.of("ok", false, "error", "不能读取目录");
        return Map.of("ok", true, "content", note.getContent() != null ? note.getContent() : "");
    }

    @PostMapping("/{id}/notes/save")
    public Map<String, Object> saveNote(@PathVariable Long id, @RequestBody Map<String, String> body) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");

        String path = body.getOrDefault("path", "").trim();
        String content = body.getOrDefault("content", "");
        if (path.isEmpty()) return Map.of("ok", false, "error", "路径不能为空");

        KbNoteEntity note = kbNoteDbService.getByKbIdAndPath(id, path);
        String now = TimeUtil.nowStr();
        if (note != null) {
            note.setContent(content);
            note.setUpdatedAt(now);
            kbNoteDbService.updateById(note);
        } else {
            String name = path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
            note = new KbNoteEntity();
            note.setKbId(id);
            note.setPath(path);
            note.setName(name);
            note.setIsDir(0);
            note.setContent(content);
            note.setCreatedAt(now);
            note.setUpdatedAt(now);
            kbNoteDbService.save(note);
            ensureParentDirs(id, path);
        }
        logService.add("保存笔记", "成功", path);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/notes/new")
    public Map<String, Object> createNote(@PathVariable Long id,
                                          @RequestParam(defaultValue = "") String dir,
                                          @RequestParam String filename,
                                          @RequestParam(defaultValue = "") String content) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");

        if (!filename.endsWith(".md")) filename += ".md";
        String path = dir.isEmpty() ? filename : dir + "/" + filename;

        KbNoteEntity existing = kbNoteDbService.getByKbIdAndPath(id, path);
        if (existing != null) return Map.of("ok", false, "error", "文件已存在");

        String now = TimeUtil.nowStr();
        KbNoteEntity note = new KbNoteEntity();
        note.setKbId(id);
        note.setPath(path);
        note.setName(filename);
        note.setIsDir(0);
        note.setContent(content);
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        kbNoteDbService.save(note);

        ensureParentDirs(id, path);
        logService.add("新建笔记", "成功", path);
        return Map.of("ok", true, "path", path);
    }

    @PostMapping("/{id}/notes/delete")
    public Map<String, Object> deleteNote(@PathVariable Long id, @RequestParam String path) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");

        KbNoteEntity note = kbNoteDbService.getByKbIdAndPath(id, path);
        if (note == null) return Map.of("ok", false, "error", "文件不存在");

        if (note.getIsDir() == 1) {
            String prefix = path + "/";
            List<KbNoteEntity> children = kbNoteDbService.listByKbId(id).stream()
                    .filter(n -> n.getPath().startsWith(prefix))
                    .collect(Collectors.toList());
            for (KbNoteEntity child : children) {
                kbNoteDbService.removeById(child.getId());
            }
        }
        kbNoteDbService.removeById(note.getId());
        logService.add("删除文件", "成功", path);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/notes/rename")
    public Map<String, Object> renameNote(@PathVariable Long id, @RequestBody Map<String, String> body) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");

        String oldPath = body.getOrDefault("oldPath", "").trim();
        String newPath = body.getOrDefault("newPath", "").trim();
        if (oldPath.isEmpty() || newPath.isEmpty()) {
            return Map.of("ok", false, "error", "路径不能为空");
        }

        KbNoteEntity note = kbNoteDbService.getByKbIdAndPath(id, oldPath);
        if (note == null) return Map.of("ok", false, "error", "原文件不存在");
        if (kbNoteDbService.getByKbIdAndPath(id, newPath) != null) {
            return Map.of("ok", false, "error", "目标路径已存在");
        }

        String now = TimeUtil.nowStr();
        if (note.getIsDir() == 1) {
            String prefix = oldPath + "/";
            List<KbNoteEntity> children = kbNoteDbService.listByKbId(id).stream()
                    .filter(n -> n.getPath().startsWith(prefix))
                    .collect(Collectors.toList());
            for (KbNoteEntity child : children) {
                child.setPath(newPath + "/" + child.getPath().substring(prefix.length()));
                child.setUpdatedAt(now);
                kbNoteDbService.updateById(child);
            }
        }
        note.setPath(newPath);
        if (newPath.contains("/")) {
            note.setName(newPath.substring(newPath.lastIndexOf("/") + 1));
        } else {
            note.setName(newPath);
        }
        note.setUpdatedAt(now);
        kbNoteDbService.updateById(note);

        ensureParentDirs(id, newPath);
        logService.add("重命名", "成功", oldPath + " -> " + newPath);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/notes/upload")
    public Map<String, Object> uploadFile(@PathVariable Long id,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam(defaultValue = "") String dir) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            return Map.of("ok", false, "error", "文件名不能为空");
        }

        String ext = documentParserService.getExtension(originalName);
        if (documentParserService.isMediaFile(originalName)) {
            return Map.of("ok", false, "error", "不支持媒体文件类型: " + ext);
        }

        String mdName = originalName.endsWith(".md") ? originalName : originalName + ".md";
        String path = dir.isEmpty() ? mdName : dir + "/" + mdName;
        if (kbNoteDbService.getByKbIdAndPath(id, path) != null) {
            return Map.of("ok", false, "error", "文件已存在: " + mdName);
        }

        try {
            byte[] data = file.getBytes();
            String content;
            if (originalName.endsWith(".md")) {
                content = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            } else if (documentParserService.isSupported(originalName)) {
                content = documentParserService.parseToMarkdown(data, originalName);
            } else {
                return Map.of("ok", false, "error", "不支持的文件类型: " + ext);
            }

            String now = TimeUtil.nowStr();
            KbNoteEntity note = new KbNoteEntity();
            note.setKbId(id);
            note.setPath(path);
            note.setName(originalName);
            note.setIsDir(0);
            note.setContent(content);
            note.setCreatedAt(now);
            note.setUpdatedAt(now);
            kbNoteDbService.save(note);
            ensureParentDirs(id, path);
            logService.add("上传文件", "成功", path);
            return Map.of("ok", true, "path", path);
        } catch (IOException e) {
            return Map.of("ok", false, "error", "上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/notes/mkdir")
    public Map<String, Object> createDir(@PathVariable Long id, @RequestParam String path) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");

        if (kbNoteDbService.getByKbIdAndPath(id, path) != null) {
            return Map.of("ok", false, "error", "路径已存在");
        }

        String name = path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
        String now = TimeUtil.nowStr();
        KbNoteEntity dir = new KbNoteEntity();
        dir.setKbId(id);
        dir.setPath(path);
        dir.setName(name);
        dir.setIsDir(1);
        dir.setContent(null);
        dir.setCreatedAt(now);
        dir.setUpdatedAt(now);
        kbNoteDbService.save(dir);

        ensureParentDirs(id, path);
        logService.add("新建目录", "成功", path);
        return Map.of("ok", true);
    }

    private void ensureParentDirs(Long kbId, String path) {
        String[] parts = path.split("/");
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (current.length() > 0) current.append("/");
            current.append(parts[i]);
            String dirPath = current.toString();
            if (kbNoteDbService.getByKbIdAndPath(kbId, dirPath) == null) {
                String now = TimeUtil.nowStr();
                KbNoteEntity dir = new KbNoteEntity();
                dir.setKbId(kbId);
                dir.setPath(dirPath);
                dir.setName(parts[i]);
                dir.setIsDir(1);
                dir.setContent(null);
                dir.setCreatedAt(now);
                dir.setUpdatedAt(now);
                kbNoteDbService.save(dir);
            }
        }
    }

    private Map<String, Object> buildTree(List<KbNoteEntity> notes) {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Map<String, Object>> dirs = new ArrayList<>();
        List<Map<String, String>> files = new ArrayList<>();

        Map<String, List<KbNoteEntity>> byParent = new LinkedHashMap<>();
        for (KbNoteEntity note : notes) {
            String p = note.getPath();
            String parent = p.contains("/") ? p.substring(0, p.lastIndexOf("/")) : "";
            byParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(note);
        }

        List<KbNoteEntity> rootItems = byParent.getOrDefault("", new ArrayList<>());
        rootItems.sort(Comparator.comparing(KbNoteEntity::getIsDir).reversed()
                .thenComparing(KbNoteEntity::getName));

        for (KbNoteEntity item : rootItems) {
            if (item.getIsDir() == 1) {
                Map<String, Object> dir = new LinkedHashMap<>();
                dir.put("name", item.getName());
                dir.put("path", item.getPath());
                dir.put("children", buildSubTree(item.getPath(), byParent));
                dirs.add(dir);
            } else {
                Map<String, String> file = new LinkedHashMap<>();
                file.put("name", item.getName());
                file.put("path", item.getPath());
                files.add(file);
            }
        }

        root.put("dirs", dirs);
        root.put("files", files);
        return root;
    }

    private Map<String, Object> buildSubTree(String parentPath, Map<String, List<KbNoteEntity>> byParent) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> dirs = new ArrayList<>();
        List<Map<String, String>> files = new ArrayList<>();

        List<KbNoteEntity> items = byParent.getOrDefault(parentPath, new ArrayList<>());
        items.sort(Comparator.comparing(KbNoteEntity::getIsDir).reversed()
                .thenComparing(KbNoteEntity::getName));

        for (KbNoteEntity item : items) {
            if (item.getIsDir() == 1) {
                Map<String, Object> dir = new LinkedHashMap<>();
                dir.put("name", item.getName());
                dir.put("path", item.getPath());
                dir.put("children", buildSubTree(item.getPath(), byParent));
                dirs.add(dir);
            } else {
                Map<String, String> file = new LinkedHashMap<>();
                file.put("name", item.getName());
                file.put("path", item.getPath());
                files.add(file);
            }
        }

        result.put("dirs", dirs);
        result.put("files", files);
        return result;
    }
}