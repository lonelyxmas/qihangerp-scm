package cn.qihang.ai.assistant.controller;

import cn.qihang.ai.assistant.service.EmbeddingService;
import cn.qihang.ai.assistant.util.TimeUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final EmbeddingService embeddingService;

    public HealthController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "version", "2.0.0",
                "time", TimeUtil.nowStr(),
                "ollama", Map.of("available", embeddingService.isAvailable())
        );
    }
}