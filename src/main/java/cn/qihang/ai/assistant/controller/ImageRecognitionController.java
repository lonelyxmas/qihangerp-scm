package cn.qihang.ai.assistant.controller;

import cn.qihang.ai.assistant.config.AppConfig;
import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.entity.LlmProfileEntity;
import cn.qihang.ai.assistant.service.KbBaseService;
import cn.qihang.ai.assistant.service.LlmConfigResolver;
import cn.qihang.ai.assistant.service.LlmService;
import cn.qihang.ai.assistant.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/image")
public class ImageRecognitionController {

    private static final Logger log = LoggerFactory.getLogger(ImageRecognitionController.class);
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp");

    private final AppConfig appConfig;
    private final KbBaseService kbService;
    private final LlmService llmService;
    private final LogService logService;
    private final LlmConfigResolver llmConfigResolver;
    private final DataSource dataSource;

    public ImageRecognitionController(AppConfig appConfig,
                                       KbBaseService kbService,
                                       LlmService llmService, LogService logService,
                                       LlmConfigResolver llmConfigResolver,
                                       DataSource dataSource) {
        this.appConfig = appConfig;
        this.kbService = kbService;
        this.llmService = llmService;
        this.logService = logService;
        this.llmConfigResolver = llmConfigResolver;
        this.dataSource = dataSource;
    }

    @GetMapping
    public String index() {
        return "redirect:/image/general";
    }

    @GetMapping("/general")
    public String page(@RequestParam(required = false) Long kbId, Model model) {
        KbBaseEntity kb = null;
        if (kbId != null) {
            kb = kbService.getById(kbId);
        }
        if (kb == null) {
            kb = kbService.getFirst();
        }

        if (kb != null) {
            model.addAttribute("currentKb", kb);
            model.addAttribute("currentKbId", kb.getId());
            model.addAttribute("currentKbName", kb.getName());
            model.addAttribute("kbReady", true);
        } else {
            model.addAttribute("currentKb", null);
            model.addAttribute("currentKbId", null);
            model.addAttribute("currentKbName", null);
            model.addAttribute("kbReady", false);
        }

        List<LlmProfileEntity> allProfiles = llmConfigResolver.getAllProfiles();
        List<LlmProfileEntity> visionModels = allProfiles.stream()
                .filter(p -> p.isMultimodal())
                .collect(Collectors.toList());
        model.addAttribute("visionModels", visionModels);
        model.addAttribute("kbList", kbService.getAll());
        model.addAttribute("currentNav", "tools");
        return "3.0/image";
    }

    // ========== SQLite CRUD ==========

    private long insertAnalysis(String imageName, String imagePath, String imageType,
                                 String prompt, String model, String source, Long kbId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO image_analyses (image_name, image_path, image_type, prompt, model, source, kb_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, 'pending', ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, imageName);
            ps.setString(2, imagePath);
            ps.setString(3, imageType);
            ps.setString(4, prompt);
            ps.setString(5, model);
            ps.setString(6, source);
            if (kbId != null) {
                ps.setLong(7, kbId);
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setString(8, java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("[识图] 插入记录失败", e);
        }
        return -1;
    }

    private void updateAnalysisResult(long id, String result, String status) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE image_analyses SET result=?, status=?, completed_at=? WHERE id=?")) {
            ps.setString(1, result);
            ps.setString(2, status);
            ps.setString(3, java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ps.setLong(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("[识图] 更新记录失败", e);
        }
    }

    private List<Map<String, Object>> getAnalysesFromDb(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM image_analyses ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getLong("id"));
                m.put("imageName", rs.getString("image_name"));
                m.put("imagePath", rs.getString("image_path"));
                m.put("imageType", rs.getString("image_type"));
                m.put("prompt", rs.getString("prompt"));
                m.put("result", rs.getString("result"));
                m.put("model", rs.getString("model"));
                m.put("source", rs.getString("source"));
                m.put("status", rs.getString("status"));
                m.put("createdAt", rs.getString("created_at"));
                m.put("completedAt", rs.getString("completed_at"));
                list.add(m);
            }
        } catch (SQLException e) {
            log.error("[识图] 查询历史失败", e);
        }
        return list;
    }

    // ========== API ==========

    @GetMapping("/api/history")
    @ResponseBody
    public Map<String, Object> getHistory(
            @RequestParam(required = false, defaultValue = "20") int limit) {
        return Map.of("ok", true, "analyses", getAnalysesFromDb(limit));
    }



    /** 上传图片并识别 */
    @PostMapping("/recognize-sync")
    @ResponseBody
    public Map<String, Object> recognizeSync(@RequestParam("image") MultipartFile image,
                                              @RequestParam(value = "prompt", defaultValue = "") String prompt,
                                              @RequestParam(value = "modelName", defaultValue = "") String modelName) {
        long startMs = System.currentTimeMillis();
        try {
            byte[] imageBytes = image.getBytes();
            String imageType = image.getContentType();
            if (imageType == null || imageType.isEmpty()) imageType = "image/jpeg";
            String fileName = image.getOriginalFilename();

            String userPrompt = (prompt == null || prompt.trim().isEmpty())
                    ? "请详细分析这张图片的内容，用中文回答。" : prompt;

            log.info("识图请求: model={}, file={}, type={}, size={}KB, prompt=\"{}\"",
                    modelName, fileName, imageType, imageBytes.length / 1024,
                    userPrompt.length() > 60 ? userPrompt.substring(0, 60) + "..." : userPrompt);

            // 创建 pending 记录
            long recordId = insertAnalysis(fileName, "", imageType, userPrompt, modelName, "upload", null);

            if (!llmService.isAvailable()) {
                if (recordId > 0) updateAnalysisResult(recordId, "LLM API Key 未配置", "failed");
                return Map.of("ok", false, "error", "LLM API Key 未配置");
            }
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String systemPrompt = "你是一个专业的图片分析助手，请根据用户指定的场景详细分析图片内容，用中文回答。";
            String reply = llmService.chatWithImage(systemPrompt, userPrompt, base64Image, imageType, modelName);

            long elapsed = System.currentTimeMillis() - startMs;
            String imageUrl = "data:" + imageType + ";base64," + base64Image;
            String result = (reply != null && !reply.isEmpty()) ? reply : "(AI 未返回结果)";

            // 更新记录
            if (recordId > 0) updateAnalysisResult(recordId, result, "completed");

            log.info("识图成功: model={}, file={}, 耗时={}ms, 响应长度={}chars",
                    modelName, fileName, elapsed, result.length());

            logService.add("识图分析", "成功", "上传图片识别");
            return Map.of("ok", true, "image_url", imageUrl, "result", result, "recordId", recordId);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startMs;
            log.error("识图失败: model={}, 耗时={}ms, 错误={}", modelName, elapsed, e.getMessage());
            return Map.of("ok", false, "error", "AI 服务调用失败: " + e.getMessage());
        }
    }



    /** 获取所有知识库 */
    @GetMapping("/api/kb-list")
    @ResponseBody
    public Map<String, Object> kbList() {
        List<KbBaseEntity> all = kbService.getAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (KbBaseEntity kb : all) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", kb.getId());
            item.put("name", kb.getName());
            result.add(item);
        }
        return Map.of("ok", true, "kbs", result);
    }

    // ========== 私有方法 ==========

    private String guessImageType(String name) {
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".bmp")) return "image/bmp";
        return "image/jpeg";
    }
}
