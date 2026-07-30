package cn.qihang.ai.assistant.controller.api;

import cn.qihang.ai.assistant.controller.BaseController;
import cn.qihang.ai.assistant.entity.KbNoteEntity;
import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.entity.KbEmbeddingEntity;
import cn.qihang.ai.assistant.entity.KbCategoryEntity;
import cn.qihang.ai.assistant.service.DocumentParserService;
import cn.qihang.ai.assistant.service.KbBaseService;
import cn.qihang.ai.assistant.service.LogService;
import cn.qihang.ai.assistant.service.storage.QiniuStorageService;
import cn.qihang.ai.assistant.service.db.KbNoteDbService;
import cn.qihang.ai.assistant.service.db.KbEmbeddingDbService;
import cn.qihang.ai.assistant.service.db.KbCategoryDbService;
import cn.qihang.ai.assistant.util.TimeUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/kb")
public class KbNoteApiController extends BaseController {

    private final KbBaseService kbService;
    private final KbNoteDbService kbNoteDbService;
    private final KbEmbeddingDbService kbEmbeddingDbService;
    private final KbCategoryDbService kbCategoryDbService;
    private final LogService logService;
    private final DocumentParserService documentParserService;
    private final QiniuStorageService qiniuStorageService;

    public KbNoteApiController(KbBaseService kbService,
                               KbNoteDbService kbNoteDbService,
                               KbEmbeddingDbService kbEmbeddingDbService,
                               KbCategoryDbService kbCategoryDbService,
                               LogService logService,
                               DocumentParserService documentParserService,
                               QiniuStorageService qiniuStorageService) {
        this.kbService = kbService;
        this.kbNoteDbService = kbNoteDbService;
        this.kbEmbeddingDbService = kbEmbeddingDbService;
        this.kbCategoryDbService = kbCategoryDbService;
        this.logService = logService;
        this.documentParserService = documentParserService;
        this.qiniuStorageService = qiniuStorageService;
    }

    // ── Tree ──

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

    // ── List ──

    @GetMapping("/{id}/notes/list")
    public Map<String, Object> listNotes(@PathVariable Long id) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");
        try {
            List<KbNoteEntity> all = kbNoteDbService.listByKbId(id);
            List<Map<String, Object>> docs = new ArrayList<>();
            for (KbNoteEntity note : all) {
                if (note.getIsDir() == 1) continue;
                int chunkCount = kbEmbeddingDbService.countByKbAndPath(id, note.getPath());
                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("id", note.getId());
                doc.put("name", note.getName());
                doc.put("path", note.getPath());
                doc.put("fileType", note.getFileType() != null ? note.getFileType() : "");
                doc.put("fileSize", note.getFileSize() != null ? note.getFileSize() : 0);
                doc.put("tags", parseTags(note.getTags()));
                doc.put("status", note.getStatus() != null ? note.getStatus() : "ready");
                doc.put("chunkCount", chunkCount);
                doc.put("createdBy", note.getCreatedBy() != null ? note.getCreatedBy() : "");
                doc.put("categoryId", note.getCategoryId());
                doc.put("createdAt", note.getCreatedAt());
                doc.put("updatedAt", note.getUpdatedAt());
                docs.add(doc);
            }
            docs.sort(Comparator.comparing(m -> (String) m.get("updatedAt"), Comparator.nullsLast(Comparator.reverseOrder())));
            return Map.of("ok", true, "documents", docs);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    // ── Chunks ──

    @GetMapping("/{id}/notes/chunks")
    public Map<String, Object> getChunks(@PathVariable Long id, @RequestParam String path) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");
        try {
            List<KbEmbeddingEntity> chunks = kbEmbeddingDbService.listByKbAndPath(id, path);
            List<Map<String, Object>> list = new ArrayList<>();
            for (KbEmbeddingEntity c : chunks) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("chunkIndex", c.getChunkIndex());
                m.put("content", c.getContent());
                m.put("pathContext", c.getPathContext() != null ? c.getPathContext() : "");
                m.put("createdAt", c.getCreatedAt());
                list.add(m);
            }
            return Map.of("ok", true, "chunks", list);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    // ── Tags ──

    @PostMapping("/{id}/notes/tags")
    public Map<String, Object> updateTags(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");
        String path = (String) body.getOrDefault("path", "");
        if (path.isEmpty()) return Map.of("ok", false, "error", "路径不能为空");
        KbNoteEntity note = kbNoteDbService.getByKbIdAndPath(id, path);
        if (note == null) return Map.of("ok", false, "error", "文件不存在");
        if (!canEdit(note)) return Map.of("ok", false, "error", "无权限: 只有创建者和管理员可编辑");
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) body.getOrDefault("tags", new ArrayList<>());
        note.setTags(toJsonArray(tags));
        note.setUpdatedAt(TimeUtil.nowStr());
        kbNoteDbService.updateById(note);
        return Map.of("ok", true, "tags", tags);
    }

    @GetMapping("/{id}/notes/tags-list")
    public Map<String, Object> getAllTags(@PathVariable Long id) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");
        try {
            List<KbNoteEntity> all = kbNoteDbService.listByKbId(id);
            Set<String> tagSet = new LinkedHashSet<>();
            for (KbNoteEntity note : all) {
                String t = note.getTags();
                if (t != null && !t.isBlank() && !t.equals("[]")) {
                    tagSet.addAll(parseTags(t));
                }
            }
            return Map.of("ok", true, "tags", new ArrayList<>(tagSet));
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    // ── Read ──

    @GetMapping("/{id}/notes/read")
    public Map<String, Object> readNote(@PathVariable Long id, @RequestParam String path) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");
        KbNoteEntity note = kbNoteDbService.getByKbIdAndPath(id, path);
        if (note == null) return Map.of("ok", false, "error", "文件不存在");
        if (note.getIsDir() == 1) return Map.of("ok", false, "error", "不能读取目录");
        return Map.of("ok", true, "content", note.getContent() != null ? note.getContent() : "");
    }

    // ── Save (edit) ──

    @PostMapping("/{id}/notes/save")
    public Map<String, Object> saveNote(@PathVariable Long id, @RequestBody Map<String, String> body) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");

        String path = body.getOrDefault("path", "").trim();
        String content = body.getOrDefault("content", "");
        if (path.isEmpty()) return Map.of("ok", false, "error", "路径不能为空");

        KbNoteEntity note = kbNoteDbService.getByKbIdAndPath(id, path);
        String now = TimeUtil.nowStr();
        String username = getUsername();
        if (note != null) {
            if (!canEdit(note)) return Map.of("ok", false, "error", "无权限: 只有创建者和管理员可编辑");
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
            note.setCreatedBy(username);
            note.setCreatedAt(now);
            note.setUpdatedAt(now);
            kbNoteDbService.save(note);
            ensureParentDirs(id, path);
        }
        logService.add("保存笔记", "成功", path);
        return Map.of("ok", true);
    }

    // ── New ──

    @PostMapping("/{id}/notes/new")
    public Map<String, Object> createNote(@PathVariable Long id,
                                          @RequestParam(defaultValue = "") String dir,
                                          @RequestParam String filename,
                                          @RequestParam(defaultValue = "") String content,
                                          @RequestParam(required = false) Long categoryId) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");

        if (!filename.endsWith(".md")) filename += ".md";
        String path = dir.isEmpty() ? filename : dir + "/" + filename;

        KbNoteEntity existing = kbNoteDbService.getByKbIdAndPath(id, path);
        if (existing != null) return Map.of("ok", false, "error", "文件已存在");

        String now = TimeUtil.nowStr();
        String username = getUsername();
        KbNoteEntity note = new KbNoteEntity();
        note.setKbId(id);
        note.setPath(path);
        note.setName(filename);
        note.setIsDir(0);
        note.setContent(content);
        note.setCreatedBy(username);
        note.setCategoryId(categoryId);
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        kbNoteDbService.save(note);

        ensureParentDirs(id, path);
        logService.add("新建笔记", "成功", path);
        return Map.of("ok", true, "path", path);
    }

    // ── Delete ──

    @PostMapping("/{id}/notes/delete")
    public Map<String, Object> deleteNote(@PathVariable Long id, @RequestParam String path) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");

        KbNoteEntity note = kbNoteDbService.getByKbIdAndPath(id, path);
        if (note == null) return Map.of("ok", false, "error", "文件不存在");
        if (!canEdit(note)) return Map.of("ok", false, "error", "无权限: 只有创建者和管理员可删除");

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

    // ── Rename ──

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
        if (!canEdit(note)) return Map.of("ok", false, "error", "无权限: 只有创建者和管理员可重命名");
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

    // ── Upload ──

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
                content = new String(data, StandardCharsets.UTF_8);
            } else if (documentParserService.isSupported(originalName)) {
                content = documentParserService.parseToMarkdown(data, originalName);
            } else {
                return Map.of("ok", false, "error", "不支持的文件类型: " + ext);
            }

            String now = TimeUtil.nowStr();
            String username = getUsername();
            KbNoteEntity note = new KbNoteEntity();
            note.setKbId(id);
            note.setPath(path);
            note.setName(originalName);
            note.setIsDir(0);
            note.setContent(content);
            note.setFileType(ext);
            note.setFileSize((long) data.length);
            note.setTags("[]");
            note.setStatus("ready");
            note.setCreatedBy(username);
            note.setCreatedAt(now);
            note.setUpdatedAt(now);

            kbNoteDbService.save(note);

            // Upload original file to Qiniu
            String qiniuKey = getQiniuKey(id, note, originalName);
            if (qiniuStorageService.isConfigured()) {
                String uploadedKey = qiniuStorageService.upload(data, qiniuKey);
                if (!uploadedKey.isEmpty()) {
                    note.setOriginalFile(qiniuKey);
                    kbNoteDbService.updateById(note);
                }
            } else {
                log.warn("Qiniu not configured, original file not saved");
            }

            ensureParentDirs(id, path);
            logService.add("上传文件", "成功", path);
            return Map.of("ok", true, "path", path);
        } catch (IOException e) {
            return Map.of("ok", false, "error", "上传失败: " + e.getMessage());
        }
    }

    // ── Download ──

    @GetMapping("/{id}/notes/download")
    public void downloadFile(@PathVariable Long id, @RequestParam String path, HttpServletResponse response) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) {
            response.setStatus(404);
            return;
        }
        KbNoteEntity note = kbNoteDbService.getByKbIdAndPath(id, path);
        if (note == null || note.getOriginalFile() == null || note.getOriginalFile().isBlank()) {
            response.setStatus(404);
            return;
        }
        String url = qiniuStorageService.downloadUrl(note.getOriginalFile());
        if (url.isEmpty()) {
            response.setStatus(404);
            return;
        }
        try {
            response.sendRedirect(url);
        } catch (IOException e) {
            log.error("下载重定向失败", e);
            response.setStatus(500);
        }
    }

    // ── Categories ──

    @GetMapping("/{id}/categories")
    public Map<String, Object> listCategories(@PathVariable Long id) {
        try {
            List<KbCategoryEntity> list = kbCategoryDbService.listByKbId(id);
            return Map.of("ok", true, "categories", list);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/{id}/categories")
    public Map<String, Object> createCategory(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "").trim();
        if (name.isEmpty()) return Map.of("ok", false, "error", "名称不能为空");
        KbCategoryEntity c = new KbCategoryEntity();
        c.setKbId(id);
        c.setName(name);
        c.setSortOrder(0);
        String now = TimeUtil.nowStr();
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        kbCategoryDbService.save(c);
        return Map.of("ok", true, "category", c);
    }

    @PutMapping("/{id}/categories/{catId}")
    public Map<String, Object> updateCategory(@PathVariable Long id, @PathVariable Long catId,
                                              @RequestBody Map<String, String> body) {
        KbCategoryEntity c = kbCategoryDbService.getById(catId);
        if (c == null) return Map.of("ok", false, "error", "分类不存在");
        String name = body.getOrDefault("name", "").trim();
        if (name.isEmpty()) return Map.of("ok", false, "error", "名称不能为空");
        c.setName(name);
        c.setUpdatedAt(TimeUtil.nowStr());
        kbCategoryDbService.updateById(c);
        return Map.of("ok", true);
    }

    @DeleteMapping("/{id}/categories/{catId}")
    public Map<String, Object> deleteCategory(@PathVariable Long id, @PathVariable Long catId) {
        KbCategoryEntity c = kbCategoryDbService.getById(catId);
        if (c == null) return Map.of("ok", false, "error", "分类不存在");
        // unlink notes
        kbNoteDbService.lambdaUpdate()
                .eq(KbNoteEntity::getCategoryId, catId)
                .set(KbNoteEntity::getCategoryId, null)
                .update();
        kbCategoryDbService.removeById(catId);
        return Map.of("ok", true);
    }

    @PutMapping("/{id}/notes/category")
    public Map<String, Object> setNoteCategory(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String path = (String) body.getOrDefault("path", "");
        if (path.isEmpty()) return Map.of("ok", false, "error", "路径不能为空");
        KbNoteEntity note = kbNoteDbService.getByKbIdAndPath(id, path);
        if (note == null) return Map.of("ok", false, "error", "文件不存在");
        Object catIdRaw = body.get("categoryId");
        Long catId = catIdRaw != null ? Long.valueOf(catIdRaw.toString()) : null;
        note.setCategoryId(catId);
        note.setUpdatedAt(TimeUtil.nowStr());
        kbNoteDbService.updateById(note);
        return Map.of("ok", true);
    }

    // ── Mkdir ──

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
        dir.setCreatedBy(getUsername());
        dir.setCreatedAt(now);
        dir.setUpdatedAt(now);
        kbNoteDbService.save(dir);

        ensureParentDirs(id, path);
        logService.add("新建目录", "成功", path);
        return Map.of("ok", true);
    }

    // ── Permission helpers ──

    private boolean canEdit(KbNoteEntity note) {
        String currentUser;
        try {
            currentUser = getUsername();
        } catch (Exception e) {
            return false;
        }
        if (currentUser == null) return false;
        if (isAdmin()) return true;
        return currentUser.equals(note.getCreatedBy());
    }

    private boolean isAdmin() {
        try {
            Long userId = getUserId();
            return userId != null && 1L == userId;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Qiniu key ──

    private String getQiniuKey(Long kbId, KbNoteEntity note, String originalName) {
        return "kb-files/" + kbId + "/" + note.getId() + "_" + originalName;
    }

    // ── Internal helpers ──

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
                dir.setCreatedBy(getUsername());
                dir.setCreatedAt(now);
                dir.setUpdatedAt(now);
                kbNoteDbService.save(dir);
            }
        }
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank() || tagsJson.equals("[]")) return new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(tagsJson, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJsonArray(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(com.fasterxml.jackson.core.io.JsonStringEncoder.getInstance().quoteAsString(tags.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
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
