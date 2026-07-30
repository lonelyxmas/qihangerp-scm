package cn.qihang.ai.assistant.controller;

import cn.qihang.ai.assistant.entity.LlmProfileEntity;
import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.service.EmbeddingService;
import cn.qihang.ai.assistant.service.FeishuService;
import cn.qihang.ai.assistant.service.KbBaseService;
import cn.qihang.ai.assistant.service.LlmConfigResolver;
import cn.qihang.ai.assistant.service.LogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/config")
public class ConfigController {

    private final LogService logService;
    private final FeishuService feishuService;
    private final EmbeddingService embeddingService;
    private final LlmConfigResolver llmConfigResolver;
    private final KbBaseService kbBaseService;

    public ConfigController(LogService logService,
                             FeishuService feishuService,
                             EmbeddingService embeddingService,
                             LlmConfigResolver llmConfigResolver,
                             KbBaseService kbBaseService) {
        this.logService = logService;
        this.feishuService = feishuService;
        this.embeddingService = embeddingService;
        this.llmConfigResolver = llmConfigResolver;
        this.kbBaseService = kbBaseService;
    }

    @GetMapping
    public String configPage() {
        return "redirect:/config/ai";
    }

    @GetMapping("/ai")
    public String configAi(Model model) {
        model.addAttribute("currentNav", "config");
        model.addAttribute("currentNavSub", "ai");
        return "3.0/config_ai";
    }

    @GetMapping("/feishu")
    public String configFeishu(Model model) {
        model.addAttribute("currentNav", "config");
        model.addAttribute("currentNavSub", "feishu");
        return "3.0/config_feishu";
    }

    @GetMapping("/system")
    public String configSystem(Model model) {
        model.addAttribute("kbCount", kbBaseService.getAll().size());
        model.addAttribute("ollamaAvailable", embeddingService.isAvailable());
        model.addAttribute("ollamaProvider", embeddingService.getProviderLabel());
        List<LlmProfileEntity> allProfiles = llmConfigResolver.getAllProfiles();
        model.addAttribute("currentModel", allProfiles.isEmpty() ? null : allProfiles.get(0).getName());
        model.addAttribute("currentNav", "config");
        model.addAttribute("currentNavSub", "system");
        return "3.0/config_system";
    }

    @GetMapping("/scheduler")
    public String configScheduler(Model model) {
        model.addAttribute("schedulerJobs", List.of(
                Map.of("id", "morning_report", "time", "每天 09:00", "desc", "生成综合日报")
        ));
        model.addAttribute("currentNav", "config");
        model.addAttribute("currentNavSub", "scheduler");
        return "3.0/config_scheduler";
    }

    @GetMapping("/kb")
    public String configKb(Model model) {
        model.addAttribute("currentNav", "config");
        model.addAttribute("currentNavSub", "kb");
        return "3.0/config_kb";
    }

    @GetMapping("/datacenter")
    public String configDatacenter(Model model) {
        model.addAttribute("currentNav", "config");
        model.addAttribute("currentNavSub", "datacenter");
        return "3.0/config_datacenter";
    }
}
