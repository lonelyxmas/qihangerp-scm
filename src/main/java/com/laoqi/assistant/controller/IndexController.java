package com.laoqi.assistant.controller;

import com.laoqi.assistant.entity.KnowledgeBaseEntity;
import com.laoqi.assistant.entity.LlmProfileEntity;
import com.laoqi.assistant.service.*;
import com.laoqi.assistant.util.MarkdownUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    private final KnowledgeBaseService kbService;
    private final TaskService taskService;
    private final ReminderService reminderService;
    private final ReportService reportService;
    private final LlmService llmService;
    private final LlmConfigResolver llmConfigResolver;

    public IndexController(KnowledgeBaseService kbService,
                           TaskService taskService,
                           ReminderService reminderService,
                           ReportService reportService,
                           LlmService llmService,
                           LlmConfigResolver llmConfigResolver) {
        this.kbService = kbService;
        this.taskService = taskService;
        this.reminderService = reminderService;
        this.reportService = reportService;
        this.llmService = llmService;
        this.llmConfigResolver = llmConfigResolver;
    }

    @GetMapping("/")
    public String home() {
//        var first = kbService.getFirst();
//        if (first != null) {
//            return "redirect:/kb/" + first.getId() + "/chat";
//        }
//        return "redirect:/config";
        return "1.0/index";
    }

    @GetMapping("/v1")
    public String index(Model model) {
        List<KnowledgeBaseEntity> allKbs = kbService.getAll();

        // 系统状态
        model.addAttribute("llmConfigured", llmService.isAvailable());
        var defaultProfile = llmConfigResolver.getDefaultProfile();
        model.addAttribute("llmName", defaultProfile != null ? defaultProfile.getName() : "");
        model.addAttribute("llmModel", defaultProfile != null ? defaultProfile.getModel() : "");
        model.addAttribute("kbCount", allKbs.size());

        // 各KB的日报和任务
        List<Map<String, Object>> kbSummaries = new ArrayList<>();
        int totalTasks = 0;
        int totalReminders = 0;

        for (KnowledgeBaseEntity kb : allKbs) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", kb.getId());
            summary.put("name", kb.getName());

            // 任务统计
            try {
                long taskCount = taskService.getAllTasks(kb.getNotesDir()).stream()
                        .filter(t -> !"done".equals(t.status))
                        .count();
                summary.put("taskCount", (int) taskCount);
                totalTasks += (int) taskCount;
            } catch (Exception e) {
                summary.put("taskCount", 0);
            }

            // 提醒统计
            try {
                int reminderCount = reminderService.getAllReminders(kb.getNotesDir()).size();
                summary.put("reminderCount", reminderCount);
                totalReminders += reminderCount;
            } catch (Exception e) {
                summary.put("reminderCount", 0);
            }

            // 日报
            try {
                if (kb.getId() != null) {
                    String report = reportService.readLatestReport(kb.getId());
                    String reportDate = reportService.getLatestReportDate(kb.getId());
                    summary.put("hasReport", report != null && !report.isEmpty());
                    summary.put("reportHtml", report != null ? MarkdownUtil.toHtml(report) : "");
                    summary.put("reportDate", reportDate);
                } else {
                    summary.put("hasReport", false);
                    summary.put("reportHtml", "");
                    summary.put("reportDate", null);
                }
            } catch (Exception e) {
                summary.put("hasReport", false);
                summary.put("reportHtml", "");
                summary.put("reportDate", null);
            }

            kbSummaries.add(summary);
        }

        model.addAttribute("kbSummaries", kbSummaries);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("totalReminders", totalReminders);
        model.addAttribute("pageTitle", "工作台");
        model.addAttribute("contentFragment", "1.0/index");
        return "1.0/layout";
    }

    @GetMapping("/v1/chat")
    public String v1Chat(@RequestParam(required = false) Long kbId, Model model) {
        List<KnowledgeBaseEntity> kbList = kbService.getAll();
        model.addAttribute("kbList", kbList);

        KnowledgeBaseEntity currentKb = null;
        if (kbId != null) {
            currentKb = kbService.getById(kbId);
        } else if (!kbList.isEmpty()) {
            currentKb = kbList.get(0);
            kbId = currentKb.getId();
        }

        if (currentKb != null) {
            model.addAttribute("kb", currentKb);
            model.addAttribute("kbId", currentKb.getId());
            model.addAttribute("kbName", currentKb.getName());
            model.addAttribute("kbReady", true);
        } else {
            model.addAttribute("kbId", 0);
            model.addAttribute("kbName", "");
            model.addAttribute("kbReady", false);
        }

        List<LlmProfileEntity> chatModels = llmConfigResolver.getAllProfiles()
                .stream()
                .filter(p -> !LlmProfileEntity.TYPE_EMBEDDING.equals(p.getModelType()))
                .collect(Collectors.toList());
        model.addAttribute("chatModels", chatModels);

        LlmProfileEntity defaultProfile = llmConfigResolver.getDefaultProfile();
        model.addAttribute("defaultModel", defaultProfile != null ? defaultProfile.getName() : "");

        if (currentKb != null) {
            try {
                var tasks = taskService.getAllTasks(currentKb.getNotesDir());
                model.addAttribute("todoHigh", tasks.stream().filter(t -> "high".equals(t.priority)).toList());
                model.addAttribute("todoMid", tasks.stream().filter(t -> "mid".equals(t.priority)).toList());
                model.addAttribute("todoLow", tasks.stream().filter(t -> "low".equals(t.priority)).toList());
                model.addAttribute("todoTotal", (int) tasks.stream().filter(t -> !"done".equals(t.status)).count());
            } catch (Exception e) {
                model.addAttribute("todoHigh", List.of());
                model.addAttribute("todoMid", List.of());
                model.addAttribute("todoLow", List.of());
                model.addAttribute("todoTotal", 0);
            }
        } else {
            model.addAttribute("todoHigh", List.of());
            model.addAttribute("todoMid", List.of());
            model.addAttribute("todoLow", List.of());
            model.addAttribute("todoTotal", 0);
        }

        model.addAttribute("pageTitle", (currentKb != null ? currentKb.getName() : "笔灵AI") + " · 对话");
        model.addAttribute("contentFragment", "1.0/chat");
        return "1.0/layout";
    }
}
