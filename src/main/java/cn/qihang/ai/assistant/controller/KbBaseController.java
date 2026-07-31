package cn.qihang.ai.assistant.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import cn.qihang.ai.assistant.entity.AiAnalysisEntity;
import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.service.NoteIndexService;
import cn.qihang.ai.assistant.service.KbIndexingService;
import cn.qihang.ai.assistant.service.db.MessageDbService;
import cn.qihang.ai.assistant.service.KbBaseService;
import cn.qihang.ai.assistant.service.LogService;
import cn.qihang.ai.assistant.service.ReportService;
import cn.qihang.ai.assistant.service.db.AiAnalysisDbService;
import cn.qihang.ai.assistant.util.FileUtil;
import cn.qihang.ai.assistant.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class KbBaseController {

    private static final Logger log = LoggerFactory.getLogger(KbBaseController.class);
    private static final TypeReference<Map<String, String>> LABELS_TYPE = new TypeReference<>() {};

    private final KbBaseService kbService;
    private final LogService logService;
    private final ReportService reportService;
    private final AiAnalysisDbService aiAnalysisDbService;
    private final NoteIndexService noteIndexService;
    private final MessageDbService messageDbService;
    private final KbIndexingService kbIndexingService;

    public KbBaseController(KbBaseService kbService,
                                   LogService logService,
                                   ReportService reportService,
                                   AiAnalysisDbService aiAnalysisDbService,
                                   NoteIndexService noteIndexService,
                                   MessageDbService messageDbService,
                                   KbIndexingService kbIndexingService) {
        this.kbService = kbService;
        this.logService = logService;
        this.reportService = reportService;
        this.aiAnalysisDbService = aiAnalysisDbService;
        this.noteIndexService = noteIndexService;
        this.messageDbService = messageDbService;
        this.kbIndexingService = kbIndexingService;
    }

    // ========== 页面路由 ==========

    @GetMapping("/kb/{id}")
    public String overview(@PathVariable Long id) {
        return "redirect:/kb/" + id + "/ai";
    }

    @GetMapping("/kb/{id}/ai")
    public String aiHub(@PathVariable Long id, Map<String, Object> model) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return "redirect:/config/kb";

        model.put("kb", kb);
        model.put("labels", parseLabels(kb.getLabels()));

        // 笔记库统计
        try {
            var stats = noteIndexService.getIndexStats(kb.getId());
            model.put("kbFileCount", stats.fileCount());
            model.put("kbIndexCount", stats.chunkCount());
        } catch (Exception e) {
            model.put("kbFileCount", 0);
            model.put("kbIndexCount", 0);
        }
        try {
            model.put("kbTotalMessages", messageDbService.countByKb(id));
        } catch (Exception e) {
            model.put("kbTotalMessages", 0);
        }

        model.put("pageTitle", "AI 指南");
        model.put("contentFragment", "2.0/kb_ai_guide");
        return "2.0/layout";
    }

    // 任务中心 /tasks，提醒中心 /reminders

    @GetMapping("/kb/{id}/index")
    public String kbIndex(@PathVariable Long id, Map<String, Object> model) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return "redirect:/config/kb";

        model.put("kb", kb);
        model.put("kbId", id);
        model.put("labels", parseLabels(kb.getLabels()));

        model.put("pageTitle", kb.getName() + " · 笔记索引");
        model.put("contentFragment", "1.0/kb_index");
        return "1.0/layout";
    }

    @GetMapping("/kb/{id}/search")
    public String kbSearch(@PathVariable Long id, Map<String, Object> model) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return "redirect:/config/kb";

        model.put("kb", kb);
        model.put("labels", parseLabels(kb.getLabels()));

        model.put("pageTitle", kb.getName() + " · 搜索");
        model.put("contentFragment", "2.0/kb_search");
        return "2.0/layout";
    }

    @GetMapping("/kb/{id}/data")
    public String kbData(@PathVariable Long id, Map<String, Object> model) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return "redirect:/config/kb";

        model.put("kb", kb);
        model.put("labels", parseLabels(kb.getLabels()));
        model.put("kbId", id);

        model.put("pageTitle", kb.getName() + " · 数据概览");
        model.put("contentFragment", "1.0/kb_data_overview");
        return "1.0/layout";
    }

    @GetMapping("/kb/{id}/data/detail")
    public String kbDataDetail(@PathVariable Long id,
                                @RequestParam(defaultValue = "") String dir,
                                @RequestParam(defaultValue = "") String file,
                                Map<String, Object> model) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return "redirect:/config/kb";

        model.put("kb", kb);
        model.put("labels", parseLabels(kb.getLabels()));
        model.put("kbId", id);
        model.put("dir", dir);
        model.put("file", file);

        model.put("pageTitle", kb.getName() + " · 数据详情");
        model.put("contentFragment", "1.0/kb_data_detail");
        return "1.0/layout";
    }


    @GetMapping("/kb/{id}/config")
    public String config(@PathVariable Long id) {
        return "redirect:/kb/" + id + "/ai";
    }

    // ========== 日报 API ==========

    @GetMapping("/kb/{id}/api/report/config")
    @ResponseBody
    public Map<String, Object> getReportConfig(@PathVariable Long id) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");
        boolean autoReport = kb.getAutoReport() != null && kb.getAutoReport() == 1;
        boolean feishuPush = kb.getFeishuPush() != null && kb.getFeishuPush() == 1;
        return Map.of(
            "ok", true,
            "autoReport", autoReport,
            "feishuPush", feishuPush
        );
    }

    @PostMapping("/kb/{id}/api/report/config")
    @ResponseBody
    public Map<String, Object> saveReportConfig(@PathVariable Long id,
                                                @RequestBody Map<String, Object> body) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("id", id);
            if (body.containsKey("autoReport")) {
                update.put("autoReport", Boolean.TRUE.equals(body.get("autoReport")));
            }
            if (body.containsKey("feishuPush")) {
                update.put("feishuPush", Boolean.TRUE.equals(body.get("feishuPush")));
            }
            kbService.save(update);
            logService.add("日报配置", "更新",
                    (body.containsKey("autoReport") ? "自动生成:" + update.get("autoReport") : "")
                    + (body.containsKey("feishuPush") ? " 飞书推送:" + update.get("feishuPush") : ""));
            return Map.of("ok", true);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage() != null ? e.getMessage() : "保存配置失败");
        }
    }

    @PostMapping("/kb/{id}/api/generate")
    @ResponseBody
    public Map<String, Object> generate(@PathVariable Long id) {
        try {
            KbBaseEntity kb = kbService.getById(id);
            if (kb == null) return Map.of("ok", false, "error", "知识库不存在");
            var r = reportService.generate(kb.getId());
            if (r.report != null) {
                reportService.saveComprehensiveReport(r.report, kb.getId());
                logService.add("手动生成日报", "成功", "知识库: " + kb.getName());
                return Map.of("ok", true);
            } else {
                logService.add("手动生成日报", "失败", r.error);
                return Map.of("ok", false, "error", r.error != null ? r.error : "AI 分析不可用");
            }
        } catch (Exception e) {
            logService.add("手动生成日报", "失败", e.getMessage());
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @GetMapping("/kb/{id}/api/report/prompt")
    @ResponseBody
    public Map<String, Object> getPrompt(@PathVariable Long id) {
        return Map.of("ok", true, "prompt", reportService.readPrompt(id));
    }

    @PostMapping("/kb/{id}/api/report/prompt")
    @ResponseBody
    public Map<String, Object> savePrompt(@PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        reportService.writePrompt(body.getOrDefault("prompt", ""), id);
        logService.add("综合日报", "保存提示词", "成功");
        return Map.of("ok", true);
    }

    @GetMapping("/kb/{id}/api/report/latest")
    @ResponseBody
    public Map<String, Object> getLatestReport(@PathVariable Long id) {
        String content = reportService.readTodayReport(id);
        if (content == null) content = reportService.readLatestReport(id);
        return Map.of("ok", true, "content", content != null ? content : "");
    }

    // ========== AI 分析 API ==========

    @GetMapping("/kb/{id}/api/analysis/list")
    @ResponseBody
    public Map<String, Object> listAnalysis(@PathVariable Long id) {
        List<AiAnalysisEntity> list = aiAnalysisDbService.lambdaQuery()
                .eq(AiAnalysisEntity::getKbId, id)
                .eq(AiAnalysisEntity::getType, "dir_analysis")
                .orderByDesc(AiAnalysisEntity::getCreatedAt)
                .list();
        List<Map<String, Object>> result = list.stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("dirPath", e.getDirPath());
            m.put("content", e.getContent());
            m.put("createdAt", e.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        return Map.of("ok", true, "list", result);
    }

    @GetMapping("/kb/{id}/api/analysis/get")
    @ResponseBody
    public Map<String, Object> getAnalysis(@PathVariable Long id, @RequestParam Long aid) {
        AiAnalysisEntity entity = aiAnalysisDbService.getById(aid);
        if (entity == null || !id.equals(entity.getKbId())) {
            return Map.of("ok", false, "error", "分析记录不存在");
        }
        return Map.of("ok", true, "content", entity.getContent(), "dirPath", entity.getDirPath() != null ? entity.getDirPath() : "");
    }

    @PostMapping("/kb/{id}/api/analysis/save")
    @ResponseBody
    public Map<String, Object> saveAnalysis(@PathVariable Long id,
                                             @RequestBody Map<String, String> body) {
        String dirPath = body.getOrDefault("dirPath", "");
        String content = body.getOrDefault("content", "");
        String prompt = body.getOrDefault("prompt", "");
        if (content.isEmpty()) return Map.of("ok", false, "error", "内容不能为空");

        AiAnalysisEntity entity = new AiAnalysisEntity();
        entity.setKbId(id);
        entity.setType("dir_analysis");
        entity.setDirPath(dirPath);
        entity.setContent(content);
        entity.setPrompt(prompt);
        String now = TimeUtil.nowStr();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        aiAnalysisDbService.save(entity);
        logService.add("AI分析", "保存", "目录: " + dirPath);
        return Map.of("ok", true);
    }

    // ========== KB API ==========

    @ResponseBody
    @GetMapping("/api/kb/list")
    public Map<String, Object> list() {
        List<KbBaseEntity> list = kbService.getAll();
        List<Map<String, Object>> result = list.stream().map(this::toMap).collect(Collectors.toList());
        return Map.of("ok", true, "list", result);
    }

    @ResponseBody
    @GetMapping("/api/kb/current")
    public Map<String, Object> current() {
        KbBaseEntity kb = kbService.getFirst();
        if (kb == null) {
            return Map.of("ok", false, "error", "未配置任何知识库");
        }
        return Map.of("ok", true, "kb", toMap(kb));
    }

    @ResponseBody
    @PostMapping("/api/kb/save")
    public Map<String, Object> save(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) {
            return Map.of("ok", false, "error", "名称不能为空");
        }

        kbService.save(body);
        logService.add("知识库", "保存", "知识库已保存: " + name);
        return Map.of("ok", true);
    }

    @ResponseBody
    @DeleteMapping("/api/kb/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        kbService.delete(id);
        logService.add("知识库", "删除", "知识库已删除: id=" + id);
        return Map.of("ok", true);
    }

    @ResponseBody
    @PostMapping("/api/kb/reorder")
    public Map<String, Object> reorder(@RequestBody List<Long> ids) {
        kbService.reorder(ids);
        logService.add("知识库", "排序", "知识库排序已更新");
        return Map.of("ok", true);
    }

    @ResponseBody
    @PostMapping("/api/kb/{id}/reindex")
    public Map<String, Object> reindex(@PathVariable Long id) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");
        if (!kbIndexingService.isAvailable()) {
            return Map.of("ok", false, "error", "Embedding 服务不可用，请先在配置页配置向量模型");
        }
        try {
            kbIndexingService.reindexKb(id);
            logService.add("知识库", "重索引", "知识库已全量重索引: " + kb.getName());
            return Map.of("ok", true);
        } catch (Exception e) {
            log.error("重索引失败", e);
            return Map.of("ok", false, "error", e.getMessage() != null ? e.getMessage() : "重索引失败");
        }
    }

    @ResponseBody
    @GetMapping("/api/kb/{id}/index-status")
    public Map<String, Object> indexStatus(@PathVariable Long id) {
        KbBaseEntity kb = kbService.getById(id);
        if (kb == null) return Map.of("ok", false, "error", "知识库不存在");
        var stats = noteIndexService.getIndexStats(kb.getId());
        var status = kbIndexingService.getKbStatus(kb.getId());
        return Map.of(
            "ok", true,
            "available", status.available(),
            "pendingCount", status.pendingCount(),
            "totalIndexed", status.totalIndexed(),
            "lastIndexTime", status.lastIndexTime() != null ? status.lastIndexTime() : "",
            "fileCount", stats.fileCount(),
            "chunkCount", stats.chunkCount(),
            "running", status.running(),
            "progress", status.progress()
        );
    }

    private Map<String, Object> toMap(KbBaseEntity e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("name", e.getName());
        m.put("labels", e.getLabels());
        m.put("sortOrder", e.getSortOrder());
        m.put("createdAt", e.getCreatedAt());
        return m;
    }

    private Map<String, String> parseLabels(String labelsJson) {
        if (labelsJson == null || labelsJson.isBlank()) return defaultLabels();
        try {
            Map<String, String> parsed = FileUtil.readJson(labelsJson, LABELS_TYPE, null);
            if (parsed == null || parsed.isEmpty()) return defaultLabels();
            Map<String, String> result = defaultLabels();
            result.putAll(parsed);
            return result;
        } catch (Exception e) {
            return defaultLabels();
        }
    }

    private Map<String, String> defaultLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put("tasks", "任务");
        labels.put("reminders", "提醒");
        labels.put("notes", "笔记");
        labels.put("config", "配置");
        return labels;
    }

}
