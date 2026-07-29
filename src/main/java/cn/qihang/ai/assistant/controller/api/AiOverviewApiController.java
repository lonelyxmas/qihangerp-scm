package cn.qihang.ai.assistant.controller.api;

import cn.qihang.ai.assistant.datacenter.AiAnalysisCache;
import cn.qihang.ai.assistant.entity.KnowledgeBaseEntity;
import cn.qihang.ai.assistant.entity.NoteEmbeddingEntity;
import cn.qihang.ai.assistant.entity.FileIndexMetaEntity;
import cn.qihang.ai.assistant.service.KnowledgeBaseService;
import cn.qihang.ai.assistant.service.LlmService;
import cn.qihang.ai.assistant.service.NoteIndexService;
import cn.qihang.ai.assistant.service.db.FileIndexMetaDbService;
import cn.qihang.ai.assistant.service.db.NoteEmbeddingDbService;
import cn.qihang.ai.assistant.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AiOverviewApiController {

    private static final Logger log = LoggerFactory.getLogger(AiOverviewApiController.class);

    private final KnowledgeBaseService kbService;
    private final NoteIndexService noteIndexService;
    private final LlmService llmService;
    private final AiAnalysisCache analysisCache;
    private final FileIndexMetaDbService fileIndexMetaDbService;
    private final NoteEmbeddingDbService noteEmbeddingDbService;

    public AiOverviewApiController(KnowledgeBaseService kbService,
                                   NoteIndexService noteIndexService,
                                   LlmService llmService,
                                   AiAnalysisCache analysisCache,
                                   FileIndexMetaDbService fileIndexMetaDbService,
                                   NoteEmbeddingDbService noteEmbeddingDbService) {
        this.kbService = kbService;
        this.noteIndexService = noteIndexService;
        this.llmService = llmService;
        this.analysisCache = analysisCache;
        this.fileIndexMetaDbService = fileIndexMetaDbService;
        this.noteEmbeddingDbService = noteEmbeddingDbService;
    }

    @GetMapping("/kb-stats")
    public ResponseEntity<Map<String, Object>> getKbStats(@RequestParam Long kbId) {
        Map<String, Object> result = new HashMap<>();
        try {
            KnowledgeBaseEntity kb = kbService.getById(kbId);
            if (kb == null) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "笔记库不存在"));
            }

            result.put("kbId", kbId);
            result.put("kbName", kb.getName());
            result.put("notesDir", kb.getNotesDir());

            var stats = noteIndexService.getIndexStats(kbId);
            result.put("fileCount", stats.fileCount());
            result.put("chunkCount", stats.chunkCount());

            List<FileIndexMetaEntity> metaList = fileIndexMetaDbService.lambdaQuery()
                    .eq(FileIndexMetaEntity::getKbId, kbId)
                    .list();

            long totalSize = metaList.stream().mapToLong(FileIndexMetaEntity::getFileSize).sum();
            result.put("totalSize", totalSize);

            Map<String, Integer> extStats = new HashMap<>();
            for (FileIndexMetaEntity meta : metaList) {
                String path = meta.getFilePath();
                String ext = path.contains(".") ? path.substring(path.lastIndexOf(".")) : ".txt";
                extStats.merge(ext, 1, Integer::sum);
            }
            result.put("extensionStats", extStats);

            List<Map<String, Object>> recentFiles = metaList.stream()
                    .sorted((a, b) -> Long.compare(b.getLastModified(), a.getLastModified()))
                    .limit(10)
                    .map(m -> {
                        Map<String, Object> fm = new HashMap<>();
                        fm.put("path", m.getFilePath());
                        fm.put("lastModified", m.getLastModified());
                        fm.put("fileSize", m.getFileSize());
                        return fm;
                    })
                    .collect(Collectors.toList());
            result.put("recentFiles", recentFiles);

            long today = System.currentTimeMillis() - 86400000;
            int todayModified = (int) metaList.stream()
                    .filter(m -> m.getLastModified() >= today)
                    .count();
            result.put("todayModified", todayModified);

            result.put("ok", true);
        } catch (Exception e) {
            log.error("获取笔记库统计失败", e);
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/projects")
    public ResponseEntity<Map<String, Object>> getProjects(@RequestParam Long kbId) {
        Map<String, Object> result = new HashMap<>();
        try {
            KnowledgeBaseEntity kb = kbService.getById(kbId);
            if (kb == null) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "笔记库不存在"));
            }

            String notesDir = kb.getNotesDir();
            if (notesDir == null || notesDir.isBlank()) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "笔记库路径未配置"));
            }

            Path baseDir = Paths.get(notesDir);
            if (!Files.exists(baseDir)) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "笔记库路径不存在"));
            }

            Set<String> ignoredDirs = noteIndexService.getIgnoredDirs(kbId);

            List<Map<String, Object>> projects = new ArrayList<>();

            try (var stream = Files.list(baseDir)) {
                List<Path> dirs = stream
                        .filter(Files::isDirectory)
                        .filter(p -> !p.getFileName().toString().startsWith("."))
                        .filter(p -> !ignoredDirs.contains(p.getFileName().toString()))
                        .sorted()
                        .collect(Collectors.toList());

                for (Path dir : dirs) {
                    Map<String, Object> project = new HashMap<>();
                    String projectName = dir.getFileName().toString();
                    project.put("name", projectName);
                    project.put("path", projectName);

                    int fileCount = 0;
                    long totalSize = 0;
                    long lastModified = 0;

                    try (var fileStream = Files.walk(dir, 10)) {
                        List<Path> files = fileStream
                                .filter(Files::isRegularFile)
                                .filter(p -> {
                                    String fn = p.getFileName().toString();
                                    return !fn.startsWith(".") && (fn.endsWith(".md") || fn.endsWith(".json") || fn.endsWith(".txt"));
                                })
                                .collect(Collectors.toList());

                        for (Path file : files) {
                            fileCount++;
                            totalSize += Files.size(file);
                            long lm = Files.getLastModifiedTime(file).toMillis();
                            if (lm > lastModified) lastModified = lm;
                        }
                    }

                    project.put("fileCount", fileCount);
                    project.put("totalSize", totalSize);
                    project.put("lastModified", lastModified);

                    String cacheKey = "project_" + kbId + "_" + projectName;
                    String analysis = analysisCache.get(cacheKey);
                    project.put("hasAnalysis", analysis != null);

                    projects.add(project);
                }
            }

            projects.sort((a, b) -> Long.compare((Long) b.get("lastModified"), (Long) a.get("lastModified")));

            result.put("ok", true);
            result.put("projects", projects);
            result.put("totalProjects", projects.size());

        } catch (Exception e) {
            log.error("获取项目列表失败", e);
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam Long kbId,
                                                      @RequestParam String query,
                                                      @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<NoteIndexService.NoteSearchResult> searchResults = noteIndexService.hybridSearch(kbId, query, limit);

            List<Map<String, Object>> results = searchResults.stream()
                    .map(r -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("filePath", r.filePath());
                        item.put("pathContext", r.pathContext());
                        item.put("content", r.content());
                        item.put("score", r.score());
                        return item;
                    })
                    .collect(Collectors.toList());

            result.put("ok", true);
            result.put("results", results);
            result.put("total", results.size());

        } catch (Exception e) {
            log.error("搜索失败", e);
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze-project")
    public ResponseEntity<Map<String, Object>> analyzeProject(@RequestParam Long kbId,
                                                               @RequestParam String projectName) {
        Map<String, Object> result = new HashMap<>();
        try {
            KnowledgeBaseEntity kb = kbService.getById(kbId);
            if (kb == null) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "笔记库不存在"));
            }

            String cacheKey = "project_" + kbId + "_" + projectName;
            String cached = analysisCache.get(cacheKey);
            if (cached != null) {
                result.put("ok", true);
                result.put("analysis", cached);
                result.put("cached", true);
                return ResponseEntity.ok(result);
            }

            String notesDir = kb.getNotesDir();
            Path projectDir = Paths.get(notesDir).resolve(projectName);
            if (!Files.exists(projectDir)) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "项目目录不存在"));
            }

            StringBuilder projectContent = new StringBuilder();
            projectContent.append("项目名称: ").append(projectName).append("\n\n");
            projectContent.append("=== 文件内容汇总 ===\n\n");

            try (var stream = Files.walk(projectDir, 10)) {
                List<Path> files = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String fn = p.getFileName().toString();
                            return !fn.startsWith(".") && (fn.endsWith(".md") || fn.endsWith(".json") || fn.endsWith(".txt"));
                        })
                        .sorted()
                        .collect(Collectors.toList());

                for (Path file : files) {
                    String relPath = projectDir.relativize(file).toString();
                    String content = FileUtil.readText(file);
                    if (content != null && !content.isBlank()) {
                        projectContent.append("--- ").append(relPath).append(" ---\n");
                        projectContent.append(content.substring(0, Math.min(1000, content.length())));
                        if (content.length() > 1000) projectContent.append("...");
                        projectContent.append("\n\n");
                    }
                }
            }

            String systemPrompt = "你是一个专业的项目分析助手。请根据提供的项目笔记内容，生成一份详细的项目分析报告。\n\n" +
                    "分析报告应包含：\n" +
                    "1. 📋 项目概览（项目目的、主要内容）\n" +
                    "2. 📊 核心数据（关键指标、统计信息）\n" +
                    "3. 🔑 关键发现（重要结论、问题点）\n" +
                    "4. 💡 建议（改进建议、下一步行动）\n" +
                    "\n使用HTML格式输出，包含适当的标题和列表。保持简洁清晰。";

            String reply = llmService.chat(systemPrompt, projectContent.toString());
            analysisCache.put(cacheKey, reply);

            result.put("ok", true);
            result.put("analysis", reply);
            result.put("cached", false);

        } catch (Exception e) {
            log.error("分析项目失败", e);
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/quick-action")
    public ResponseEntity<Map<String, Object>> quickAction(@RequestParam Long kbId,
                                                           @RequestParam String action) {
        Map<String, Object> result = new HashMap<>();
        try {
            KnowledgeBaseEntity kb = kbService.getById(kbId);
            if (kb == null) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "笔记库不存在"));
            }

            String cacheKey = "action_" + kbId + "_" + action;
            String cached = analysisCache.get(cacheKey);
            if (cached != null) {
                result.put("ok", true);
                result.put("result", cached);
                result.put("cached", true);
                return ResponseEntity.ok(result);
            }

            String notesDir = kb.getNotesDir();

            String systemPrompt;
            String userMessage = "";

            switch (action) {
                case "summarize-week":
                    systemPrompt = "你是一个专业的学习助手。请根据最近一周的笔记内容，生成一份周总结报告。\n\n" +
                            "报告应包含：\n" +
                            "1. 📅 本周概览\n" +
                            "2. 📚 学习内容总结\n" +
                            "3. 🔑 重要知识点\n" +
                            "4. 🎯 下周建议\n" +
                            "\n使用HTML格式输出。";
                    userMessage = collectRecentNotes(kbId, 7);
                    break;

                case "extract-key-points":
                    systemPrompt = "你是一个专业的知识提取助手。请从笔记内容中提取关键知识点。\n\n" +
                            "要求：\n" +
                            "1. 识别核心概念和重要术语\n" +
                            "2. 提取关键数据和结论\n" +
                            "3. 整理知识结构关系\n" +
                            "\n使用HTML格式输出，用列表展示。";
                    userMessage = collectAllNotes(kbId);
                    break;

                case "generate-review-plan":
                    systemPrompt = "你是一个专业的学习规划师。请根据笔记内容，生成一个复习计划。\n\n" +
                            "计划应包含：\n" +
                            "1. 📋 知识点清单\n" +
                            "2. 📅 复习时间表\n" +
                            "3. 🎯 重点难点\n" +
                            "4. ✅ 自测建议\n" +
                            "\n使用HTML格式输出。";
                    userMessage = collectAllNotes(kbId);
                    break;

                case "find-related":
                    systemPrompt = "你是一个知识关联助手。请分析笔记内容，找出相关联的知识点和主题。\n\n" +
                            "要求：\n" +
                            "1. 识别主题聚类\n" +
                            "2. 找出跨文件的知识关联\n" +
                            "3. 发现潜在的知识缺口\n" +
                            "\n使用HTML格式输出。";
                    userMessage = collectAllNotes(kbId);
                    break;

                default:
                    return ResponseEntity.ok(Map.of("ok", false, "error", "不支持的操作: " + action));
            }

            String reply = llmService.chat(systemPrompt, userMessage);
            analysisCache.put(cacheKey, reply);

            result.put("ok", true);
            result.put("result", reply);
            result.put("cached", false);

        } catch (Exception e) {
            log.error("执行快捷操作失败", e);
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/heatmap")
    public ResponseEntity<Map<String, Object>> getHeatmap(@RequestParam Long kbId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<FileIndexMetaEntity> metaList = fileIndexMetaDbService.lambdaQuery()
                    .eq(FileIndexMetaEntity::getKbId, kbId)
                    .list();

            Map<String, Integer> heatmap = new HashMap<>();
            for (FileIndexMetaEntity meta : metaList) {
                long lm = meta.getLastModified();
                String date = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(lm));
                heatmap.merge(date, 1, Integer::sum);
            }

            result.put("ok", true);
            result.put("heatmap", heatmap);

        } catch (Exception e) {
            log.error("获取热力图数据失败", e);
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/tags")
    public ResponseEntity<Map<String, Object>> getTags(@RequestParam Long kbId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<NoteEmbeddingEntity> embeddings = noteEmbeddingDbService.lambdaQuery()
                    .eq(NoteEmbeddingEntity::getKbId, kbId)
                    .list();

            Map<String, Integer> tagCounts = new HashMap<>();
            for (NoteEmbeddingEntity e : embeddings) {
                String content = e.getContent();
                if (content != null) {
                    String[] words = content.split("[\\s\\p{Punct}]+");
                    for (String word : words) {
                        if (word.length() >= 2 && word.length() <= 15) {
                            tagCounts.merge(word, 1, Integer::sum);
                        }
                    }
                }
            }

            List<Map<String, Object>> tags = tagCounts.entrySet().stream()
                    .filter(e -> e.getValue() >= 2)
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(50)
                    .map(e -> {
                        Map<String, Object> tag = new HashMap<>();
                        tag.put("name", e.getKey());
                        tag.put("count", e.getValue());
                        return tag;
                    })
                    .collect(Collectors.toList());

            result.put("ok", true);
            result.put("tags", tags);

        } catch (Exception e) {
            log.error("获取标签失败", e);
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    private String collectRecentNotes(Long kbId, int days) {
        long cutoff = System.currentTimeMillis() - (days * 86400000L);
        List<FileIndexMetaEntity> metaList = fileIndexMetaDbService.lambdaQuery()
                .eq(FileIndexMetaEntity::getKbId, kbId)
                .gt(FileIndexMetaEntity::getLastModified, cutoff)
                .list();

        StringBuilder content = new StringBuilder();
        content.append("最近").append(days).append("天的笔记内容：\n\n");

        KnowledgeBaseEntity kb = kbService.getById(kbId);
        String notesDir = kb != null ? kb.getNotesDir() : "";

        for (FileIndexMetaEntity meta : metaList) {
            try {
                Path filePath = Paths.get(notesDir).resolve(meta.getFilePath());
                String text = FileUtil.readText(filePath);
                if (text != null && !text.isBlank()) {
                    content.append("=== ").append(meta.getFilePath()).append(" ===\n");
                    content.append(text.substring(0, Math.min(500, text.length())));
                    if (text.length() > 500) content.append("...");
                    content.append("\n\n");
                }
            } catch (Exception e) {
                log.warn("读取文件失败: {}", meta.getFilePath());
            }
        }

        return content.toString();
    }

    private String collectAllNotes(Long kbId) {
        List<FileIndexMetaEntity> metaList = fileIndexMetaDbService.lambdaQuery()
                .eq(FileIndexMetaEntity::getKbId, kbId)
                .list();

        StringBuilder content = new StringBuilder();
        content.append("笔记库所有内容：\n\n");

        KnowledgeBaseEntity kb = kbService.getById(kbId);
        String notesDir = kb != null ? kb.getNotesDir() : "";

        for (FileIndexMetaEntity meta : metaList) {
            try {
                Path filePath = Paths.get(notesDir).resolve(meta.getFilePath());
                String text = FileUtil.readText(filePath);
                if (text != null && !text.isBlank()) {
                    content.append("=== ").append(meta.getFilePath()).append(" ===\n");
                    content.append(text.substring(0, Math.min(300, text.length())));
                    if (text.length() > 300) content.append("...");
                    content.append("\n\n");
                }
            } catch (Exception e) {
                log.warn("读取文件失败: {}", meta.getFilePath());
            }
        }

        return content.toString();
    }
}
