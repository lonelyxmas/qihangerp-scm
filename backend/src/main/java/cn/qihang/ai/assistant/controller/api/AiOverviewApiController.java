package cn.qihang.ai.assistant.controller.api;

import cn.qihang.ai.assistant.datacenter.AiAnalysisCache;
import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.entity.KbEmbeddingEntity;
import cn.qihang.ai.assistant.security.common.SecurityUtils;
import cn.qihang.ai.assistant.service.KbBaseService;
import cn.qihang.ai.assistant.service.LlmService;
import cn.qihang.ai.assistant.service.NoteIndexService;
import cn.qihang.ai.assistant.service.db.KbEmbeddingDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AiOverviewApiController {

    private static final Logger log = LoggerFactory.getLogger(AiOverviewApiController.class);

    private final KbBaseService kbService;
    private final NoteIndexService noteIndexService;
    private final LlmService llmService;
    private final AiAnalysisCache analysisCache;
    private final KbEmbeddingDbService noteEmbeddingDbService;

    public AiOverviewApiController(KbBaseService kbService,
                                   NoteIndexService noteIndexService,
                                   LlmService llmService,
                                   AiAnalysisCache analysisCache,
                                   KbEmbeddingDbService noteEmbeddingDbService) {
        this.kbService = kbService;
        this.noteIndexService = noteIndexService;
        this.llmService = llmService;
        this.analysisCache = analysisCache;
        this.noteEmbeddingDbService = noteEmbeddingDbService;
    }

    @GetMapping("/kb-stats")
    public ResponseEntity<Map<String, Object>> getKbStats(@RequestParam Long kbId) {
        Map<String, Object> result = new HashMap<>();
        try {
            KbBaseEntity kb = kbService.getById(kbId);
            if (kb == null) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "笔记库不存在"));
            }

            result.put("kbId", kbId);
            result.put("kbName", kb.getName());

            var stats = noteIndexService.getIndexStats(kbId);
            result.put("fileCount", stats.fileCount());
            result.put("chunkCount", stats.chunkCount());

            
            result.put("ok", true);
        } catch (Exception e) {
            log.error("获取笔记库统计失败", e);
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

    @GetMapping("/global-search")
    public ResponseEntity<Map<String, Object>> globalSearch(@RequestParam String query,
                                                             @RequestParam(defaultValue = "15") int limit) {
        Map<String, Object> result = new HashMap<>();
        if (!SecurityUtils.isLoggedIn()) {
            result.put("ok", false);
            result.put("error", "请先登录");
            return ResponseEntity.ok(result);
        }
        try {
            List<NoteIndexService.GlobalSearchResult> searchResults = noteIndexService.globalSearch(query, limit);

            // 获取 KB 名称映射
            Map<Long, String> kbNameMap = new HashMap<>();
            for (NoteIndexService.GlobalSearchResult r : searchResults) {
                if (!kbNameMap.containsKey(r.kbId())) {
                    KbBaseEntity kb = kbService.getById(r.kbId());
                    kbNameMap.put(r.kbId(), kb != null ? kb.getName() : "未知知识库");
                }
            }

            List<Map<String, Object>> results = searchResults.stream()
                    .map(r -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("kbId", r.kbId());
                        item.put("kbName", kbNameMap.get(r.kbId()));
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
            log.error("全局搜索失败", e);
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/tags")
    public ResponseEntity<Map<String, Object>> getTags(@RequestParam Long kbId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<KbEmbeddingEntity> embeddings = noteEmbeddingDbService.lambdaQuery()
                    .eq(KbEmbeddingEntity::getKbId, kbId)
                    .list();

            Map<String, Integer> tagCounts = new HashMap<>();
            for (KbEmbeddingEntity e : embeddings) {
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

}
