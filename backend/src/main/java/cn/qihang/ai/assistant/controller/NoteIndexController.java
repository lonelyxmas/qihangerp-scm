package cn.qihang.ai.assistant.controller;

import cn.qihang.ai.assistant.service.LogService;
import cn.qihang.ai.assistant.service.NoteIndexService;
import cn.qihang.ai.assistant.service.NoteIndexService.NoteSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/kb/{id}/api/index")
public class NoteIndexController {

    private static final Logger log = LoggerFactory.getLogger(NoteIndexController.class);

    private final NoteIndexService noteIndexService;
    private final LogService logService;

    public NoteIndexController(NoteIndexService noteIndexService, LogService logService) {
        this.noteIndexService = noteIndexService;
        this.logService = logService;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus(@PathVariable Long id) {
        boolean embeddingAvailable = noteIndexService.isAvailable();
        var stats = noteIndexService.getIndexStats(id);
        
        String status;
        if (!embeddingAvailable) {
            status = "unavailable";
        } else if (stats.fileCount() == 0) {
            status = "empty";
        } else {
            status = "ready";
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("ok", true);
        result.put("embeddingAvailable", embeddingAvailable);
        result.put("status", status);
        result.put("fileCount", stats.fileCount());
        result.put("chunkCount", stats.chunkCount());

        return result;
    }

    @PostMapping("/search")
    public Map<String, Object> search(@PathVariable Long id,
                                       @RequestBody Map<String, Object> body) {
        String query = (String) body.get("query");
        int limit = body.containsKey("limit") ? (int) body.get("limit") : 5;

        if (query == null || query.isBlank()) {
            return Map.of("ok", false, "error", "搜索内容不能为空");
        }

        if (!noteIndexService.isAvailable()) {
            return Map.of("ok", false, "error", "Embedding 服务不可用");
        }

        try {
            List<NoteSearchResult> results = noteIndexService.hybridSearch(id, query, limit);
            for (NoteSearchResult r : results) {
                log.info("[NoteIndex] 搜索结果: file={}, score={}, content={}", 
                        r.filePath(), String.format("%.3f", r.score()), 
                        r.content().length() > 50 ? r.content().substring(0, 50) + "..." : r.content());
            }
            return Map.of("ok", true, "results", results, "query", query);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/hybrid-search")
    public Map<String, Object> hybridSearch(@PathVariable Long id,
                                              @RequestBody Map<String, Object> body) {
        String query = (String) body.get("query");
        int limit = body.containsKey("limit") ? (int) body.get("limit") : 5;

        if (query == null || query.isBlank()) {
            return Map.of("ok", false, "error", "搜索内容不能为空");
        }

        if (!noteIndexService.isAvailable()) {
            return Map.of("ok", false, "error", "Embedding 服务不可用");
        }

        try {
            List<NoteSearchResult> results = noteIndexService.hybridSearch(id, query, limit);
            return Map.of("ok", true, "results", results, "query", query);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @DeleteMapping
    public Map<String, Object> clearIndex(@PathVariable Long id) {
        noteIndexService.clearIndex(id);
        logService.add("笔记索引", "清空", "知识库: " + id);
        return Map.of("ok", true, "message", "索引已清空");
    }
}
