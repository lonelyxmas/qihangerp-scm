package cn.qihang.ai.assistant.controller.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.qihang.ai.assistant.datacenter.AiAnalysisCache;
import cn.qihang.ai.assistant.datacenter.DataModuleService;
import cn.qihang.ai.assistant.datacenter.DataSetImportService;
import cn.qihang.ai.assistant.datacenter.DataSetService;
import cn.qihang.ai.assistant.datacenter.model.*;
import cn.qihang.ai.assistant.service.LlmService;

import cn.qihang.ai.assistant.entity.SysUser;
import cn.qihang.ai.assistant.security.LoginUser;
import cn.qihang.ai.assistant.security.TokenService;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/datacenter")
public class DataApiController {

    private static final Logger log = LoggerFactory.getLogger(DataApiController.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");

    private final DataSetService dataSetService;
    private final DataModuleService moduleService;
    private final DataSetImportService importService;
    private final LlmService llmService;
    private final AiAnalysisCache analysisCache;
    private final TokenService tokenService;

    public DataApiController(DataSetService dataSetService,
                             DataModuleService moduleService,
                             DataSetImportService importService,
                             LlmService llmService,
                             AiAnalysisCache analysisCache,
                             TokenService tokenService) {
        this.dataSetService = dataSetService;
        this.moduleService = moduleService;
        this.importService = importService;
        this.llmService = llmService;
        this.analysisCache = analysisCache;
        this.tokenService = tokenService;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private LoginUser getLoginUser() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) return null;
        return tokenService.getLoginUser(request);
    }

    private void requireLogin() {
        if (getLoginUser() == null) {
            throw new IllegalStateException("未登录");
        }
    }

    private long getCurrentUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUserId() : 0L;
    }

    private String getCurrentUserName() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) return null;
        SysUser user = loginUser.getUser();
        return user != null ? user.getUserName() : null;
    }

    private void requireAdmin() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            throw new IllegalStateException("未登录");
        }
        if (!SysUser.isAdmin(loginUser.getUserId())) {
            throw new IllegalStateException("仅管理员可执行此操作");
        }
    }

    @GetMapping("/check-auth")
    public ResponseEntity<Map<String, Object>> checkAuth() {
        if (getLoginUser() != null) {
            return ResponseEntity.ok(Map.of("ok", true));
        }
        return ResponseEntity.status(401).body(Map.of("ok", false, "error", "未登录"));
    }

    private String doAiChat(String systemPrompt, String userMessage) throws Exception {
        if (!llmService.isAvailable()) {
            throw new IllegalStateException("LLM API Key 未配置");
        }
        return llmService.chat(systemPrompt, userMessage);
    }

    // ========== LLM Chat API ==========
    @PostMapping("/llm/chat")
    public ResponseEntity<Map<String, Object>> llmChat(@RequestBody Map<String, String> body) {
        try {
            String system = body.getOrDefault("system", "你是一个数据分析助手。");
            String user = body.get("user");
            if (user == null || user.isBlank()) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "消息不能为空"));
            }
            String reply = doAiChat(system, user);
            return ResponseEntity.ok(Map.of("ok", true, "reply", reply));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    // ========== AI Analysis API ==========
    @GetMapping("/modules/{moduleId}/analysis")
    public ResponseEntity<Map<String, Object>> getModuleAnalysis(@PathVariable String moduleId) {
        String cached = analysisCache.get(moduleId);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("cached", cached != null);
        result.put("reply", cached);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/modules/{moduleId}/analysis")
    public ResponseEntity<Map<String, Object>> generateModuleAnalysis(
            @PathVariable String moduleId,
            @RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> datasets = (List<Map<String, Object>>) body.get("datasets");

            StringBuilder summary = new StringBuilder();
            summary.append("模块数据概览分析：\n\n");

            if (datasets != null) {
                for (Map<String, Object> ds : datasets) {
                    summary.append("数据集：").append(ds.get("name")).append("（").append(ds.get("count")).append("条记录）\n");
                    if (ds.get("fields") != null) {
                        summary.append("字段：").append(ds.get("fields")).append("\n");
                    }

                    if (ds.get("statusCount") != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> statusCount = (Map<String, Object>) ds.get("statusCount");
                        summary.append("状态分布：");
                        for (Map.Entry<String, Object> e : statusCount.entrySet()) {
                            summary.append(e.getKey()).append("=").append(e.getValue()).append(" ");
                        }
                        summary.append("\n");
                    }

                    if (ds.get("trendDesc") != null) {
                        summary.append("时间趋势：").append(ds.get("trendDesc")).append("\n");
                    }

                    if (ds.get("fieldDistributions") != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> fieldDistributions = (Map<String, Object>) ds.get("fieldDistributions");
                        for (Map.Entry<String, Object> e : fieldDistributions.entrySet()) {
                            summary.append("字段[").append(e.getKey()).append("]分布：");
                            @SuppressWarnings("unchecked")
                            List<List<Object>> vals = (List<List<Object>>) e.getValue();
                            for (List<Object> pair : vals) {
                                summary.append(pair.get(0)).append("(").append(pair.get(1)).append(") ");
                            }
                            summary.append("\n");
                        }
                    }

                    if (ds.get("sample") != null) {
                        summary.append("样本数据：").append(ds.get("sample")).append("\n");
                    }
                    summary.append("\n");
                }
            }

            String systemPrompt = "你是一个数据分析助手。请根据提供的数据生成详细的分析报告，包括：\n"
                    + "1. 数据概览（总量、各数据集分布）\n"
                    + "2. 状态分析（完成率、各状态占比、瓶颈环节）\n"
                    + "3. 趋势分析（随时间变化趋势、忙碌/空闲期、增长/下降）\n"
                    + "4. 关键发现（亮点数据、异常情况、值得关注的点）\n"
                    + "5. 建议（基于数据的具体可执行建议）\n"
                    + "使用简洁的中文，用HTML格式输出（用<h4>、<p>、<ul><li>等标签）。最后加一句鼓励的话。";

            String reply = doAiChat(systemPrompt, summary.toString());
            analysisCache.put(moduleId, reply);

            return ResponseEntity.ok(Map.of("ok", true, "reply", reply, "cached", false));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/modules/{moduleId}/analysis")
    public ResponseEntity<Map<String, Object>> clearModuleAnalysis(@PathVariable String moduleId) {
        analysisCache.invalidate(moduleId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ========== Module APIs ==========

    @GetMapping("/modules")
    public ResponseEntity<Map<String, Object>> listModules() {
        return ResponseEntity.ok(Map.of("ok", true, "data", moduleService.getAllModules()));
    }

    @GetMapping("/modules/{id}")
    public ResponseEntity<Map<String, Object>> getModule(@PathVariable String id) {
        Map<String, Object> module = moduleService.getModule(id);
        if (module == null) {
            return ResponseEntity.ok(Map.of("ok", false, "error", "模块不存在"));
        }
        return ResponseEntity.ok(Map.of("ok", true, "data", module));
    }

    @PostMapping("/modules")
    public ResponseEntity<Map<String, Object>> createModule(@RequestBody Map<String, String> body) {
        try {
            requireAdmin();
            String name = body.get("name");
            String description = body.get("description");
            String icon = body.get("icon");
            Map<String, Object> module = moduleService.createModule(name, description, icon);
            return ResponseEntity.ok(Map.of("ok", true, "data", module));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/modules/{id}")
    public ResponseEntity<Map<String, Object>> updateModule(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            requireAdmin();
            String name = (String) body.get("name");
            String description = (String) body.get("description");
            String icon = (String) body.get("icon");
            Integer sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : null;
            Map<String, Object> module = moduleService.updateModule(id, name, description, icon, sortOrder);
            if (module == null) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "模块不存在"));
            }
            return ResponseEntity.ok(Map.of("ok", true, "data", module));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/modules/{id}")
    public ResponseEntity<Map<String, Object>> deleteModule(@PathVariable String id) {
        try {
            requireAdmin();
            boolean deleted = moduleService.deleteModule(id);
            if (!deleted) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "模块不存在"));
            }
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/modules/{id}/datasets")
    public ResponseEntity<Map<String, Object>> getModuleDatasets(@PathVariable String id) {
        List<DataSet> allDatasets = dataSetService.getAllDatasets();
        List<DataSet> moduleDatasets = allDatasets.stream()
            .filter(ds -> id.equals(ds.getModuleId()))
            .toList();
        return ResponseEntity.ok(Map.of("ok", true, "data", moduleDatasets, "total", moduleDatasets.size()));
    }

    @GetMapping("/datasets")
    public ResponseEntity<Map<String, Object>> listDatasets() {
        return ResponseEntity.ok(Map.of("ok", true, "data", dataSetService.getAllDatasets()));
    }

    @GetMapping("/datasets/{id}")
    public ResponseEntity<Map<String, Object>> getDataset(@PathVariable String id) {
        DataSet ds = dataSetService.getDataset(id);
        if (ds == null) {
            return ResponseEntity.ok(Map.of("ok", false, "error", "数据集不存在"));
        }
        return ResponseEntity.ok(Map.of("ok", true, "data", ds));
    }

    @PostMapping("/datasets")
    public ResponseEntity<Map<String, Object>> createDataset(@RequestBody DataSet ds, HttpServletRequest request) {
        try {
            requireAdmin();
            DataSet created = dataSetService.createDataset(ds);
            return ResponseEntity.ok(Map.of("ok", true, "data", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/datasets/{id}")
    public ResponseEntity<Map<String, Object>> updateDataset(@PathVariable String id, @RequestBody DataSet ds, HttpServletRequest request) {
        try {
            requireAdmin();
            DataSet updated = dataSetService.updateDataset(id, ds);
            if (updated == null) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "数据集不存在"));
            }
            return ResponseEntity.ok(Map.of("ok", true, "data", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/datasets/{id}")
    public ResponseEntity<Map<String, Object>> deleteDataset(@PathVariable String id, HttpServletRequest request) {
        try {
            requireAdmin();
            boolean deleted = dataSetService.deleteDataset(id);
            if (!deleted) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "数据集不存在"));
            }
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/datasets/{id}/records")
    public ResponseEntity<Map<String, Object>> getRecords(
            @PathVariable String id,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<Map<String, Object>> records;
            int total;
            if (keyword != null && !keyword.isBlank()) {
                records = dataSetService.searchRecords(id, keyword);
                total = records.size();
                int offset = page * size;
                if (offset < records.size()) {
                    records = records.subList(offset, Math.min(offset + size, records.size()));
                } else {
                    records = new ArrayList<>();
                }
            } else {
                total = dataSetService.countRecords(id);
                records = dataSetService.loadRecords(id, page * size, size);
            }
            return ResponseEntity.ok(Map.of("ok", true, "data", records, "total", total, "page", page, "size", size));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/datasets/{id}/records/{recordId}")
    public ResponseEntity<Map<String, Object>> deleteRecord(@PathVariable String id, @PathVariable String recordId) {
        try {
            requireLogin();
            boolean deleted = dataSetService.deleteRecord(id, recordId);
            if (!deleted) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "记录不存在"));
            }
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/datasets/{id}/records")
    public ResponseEntity<Map<String, Object>> createRecord(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            requireLogin();
            DataSet ds = dataSetService.getDataset(id);
            if (ds == null) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "数据集不存在"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> record = (Map<String, Object>) body.get("data");
            if (record == null) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "记录数据不能为空"));
            }

            long userId = getCurrentUserId();
            String userName = getCurrentUserName();
            int count = dataSetService.addRecords(id, List.of(record), "manual", userId, userName);
            return ResponseEntity.ok(Map.of("ok", true, "count", count));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/datasets/{id}/records/{recordId}")
    public ResponseEntity<Map<String, Object>> updateRecord(
            @PathVariable String id,
            @PathVariable String recordId,
            @RequestBody Map<String, Object> body) {
        try {
            requireLogin();
            DataSet ds = dataSetService.getDataset(id);
            if (ds == null) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "数据集不存在"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) body.get("data");

            long userId = getCurrentUserId();
            String userName = getCurrentUserName();
            Map<String, Object> updated = dataSetService.updateRecord(id, recordId, data, userId, userName);
            if (updated == null) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "记录不存在"));
            }

            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @PatchMapping("/datasets/{id}/records/{recordId}/status")
    public ResponseEntity<Map<String, Object>> updateRecordStatus(
            @PathVariable String id,
            @PathVariable String recordId,
            @RequestBody Map<String, String> body) {
        try {
            requireLogin();
            String status = body.get("status");
            if (status == null || status.isBlank()) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "状态不能为空"));
            }
            long userId = getCurrentUserId();
            String userName = getCurrentUserName();
            boolean updated = dataSetService.updateRecordField(id, recordId, "status", status, userId, userName);
            if (!updated) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "记录不存在"));
            }
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/datasets/{id}/import/excel")
    public ResponseEntity<Map<String, Object>> importExcel(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mapping", required = false) String mappingJson,
            @RequestParam(value = "useAi", defaultValue = "false") boolean useAi) {
        try {
            DataSet ds = dataSetService.getDataset(id);
            if (ds == null) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "数据集不存在"));
            }

            Map<String, String> mapping = new HashMap<>();
            if (mappingJson != null && !mappingJson.isEmpty()) {
                mapping = mapper.readValue(mappingJson, new TypeReference<Map<String, String>>() {});
            }

            List<Map<String, Object>> records;
            if (mapping.isEmpty()) {
                records = importService.importExcelWithAutoDetect(file, ds.getSchema());
            } else {
                records = importService.importExcel(file, mapping);
            }

            if (useAi && !records.isEmpty()) {
                try {
                    records = normalizeWithAi(records, ds);
                } catch (Exception e) {
                    return ResponseEntity.ok(Map.of("ok", false, "error", "AI 标准化失败: " + e.getMessage()));
                }
            }

            requireLogin();
            long userId = getCurrentUserId();
            String userName = getCurrentUserName();
            int count = dataSetService.addRecords(id, records, "excel", userId, userName);
            return ResponseEntity.ok(Map.of("ok", true, "count", count, "message", "成功导入 " + count + " 条记录"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ok", false, "error", "导入失败: " + e.getMessage()));
        }
    }

    @PostMapping("/datasets/import/excel/preview")
    public ResponseEntity<Map<String, Object>> previewExcel(@RequestParam("file") MultipartFile file) {
        try {
            DataSetImportService.ExcelPreview preview = importService.previewExcel(file);
            return ResponseEntity.ok(Map.of("ok", true, "data", preview));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ok", false, "error", "预览失败: " + e.getMessage()));
        }
    }

    @PostMapping("/datasets/{id}/import/json")
    public ResponseEntity<Map<String, Object>> importJson(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            DataSet ds = dataSetService.getDataset(id);
            if (ds == null) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "数据集不存在"));
            }

            Object dataObj = body.get("data");
            boolean useAi = Boolean.TRUE.equals(body.get("useAi"));
            List<Map<String, Object>> records = new ArrayList<>();

            if (dataObj instanceof List<?> dataList) {
                for (Object item : dataList) {
                    if (item instanceof Map<?, ?> map) {
                        Map<String, Object> record = new HashMap<>();
                        map.forEach((k, v) -> record.put(String.valueOf(k), v));
                        records.add(record);
                    }
                }
            } else if (dataObj instanceof Map<?, ?> map) {
                Map<String, Object> record = new HashMap<>();
                map.forEach((k, v) -> record.put(String.valueOf(k), v));
                records.add(record);
            }

            if (useAi && !records.isEmpty()) {
                try {
                    records = normalizeWithAi(records, ds);
                } catch (Exception e) {
                    return ResponseEntity.ok(Map.of("ok", false, "error", "AI 标准化失败: " + e.getMessage()));
                }
            }

            requireLogin();
            long userId = getCurrentUserId();
            String userName = getCurrentUserName();
            int count = dataSetService.addRecords(id, records, "manual", userId, userName);
            return ResponseEntity.ok(Map.of("ok", true, "count", count, "message", "成功导入 " + count + " 条记录"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ok", false, "error", "导入失败: " + e.getMessage()));
        }
    }

    @PostMapping("/datasets/{id}/import/url")
    public ResponseEntity<Map<String, Object>> importFromUrl(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            DataSet ds = dataSetService.getDataset(id);
            if (ds == null) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "数据集不存在"));
            }

            String url = body.get("url");
            if (url == null || url.isBlank()) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "URL不能为空"));
            }

            String prompt = "请访问以下URL，提取页面中的结构化数据。\n" +
                    "URL: " + url + "\n\n" +
                    "**重要：不要保存到文件，不要写入任何文件。只在回复中直接输出JSON数据。**\n\n" +
                    "输出格式要求：\n" +
                    "1. 直接输出JSON数组，不要包含其他文字说明\n" +
                    "2. 用 ```json 包裹\n" +
                    "3. 每个元素是一个对象，包含页面中的数据字段";

            if (ds.getSchema() != null && ds.getSchema().getFields() != null && !ds.getSchema().getFields().isEmpty()) {
                prompt += "\n\n必须使用以下字段名（保持英文）：\n";
                for (DataField field : ds.getSchema().getFields()) {
                    prompt += "- " + field.getName();
                    if (field.getDisplayName() != null) {
                        prompt += "（" + field.getDisplayName() + "）";
                    }
                    prompt += "\n";
                }
                prompt += "\n示例格式：[{\"title\":\"文章标题\",\"views\":123}, ...]";
            }

            String rawResponse = doAiChat("你是一个数据采集助手。请严格按要求的格式输出JSON数据。", prompt);
            String parsedData = extractJsonFromResponse(rawResponse);

            Object jsonData = mapper.readValue(parsedData, Object.class);
            List<Map<String, Object>> records = new ArrayList<>();

            if (jsonData instanceof List<?> dataList) {
                for (Object item : dataList) {
                    if (item instanceof Map<?, ?> map) {
                        Map<String, Object> record = new HashMap<>();
                        map.forEach((k, v) -> record.put(String.valueOf(k), v));
                        records.add(record);
                    }
                }
            }

            if (records.isEmpty()) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "未能从URL提取到有效数据"));
            }

            requireLogin();
            long userId = getCurrentUserId();
            String userName = getCurrentUserName();
            int count = dataSetService.addRecords(id, records, "url", userId, userName);
            return ResponseEntity.ok(Map.of("ok", true, "count", count, "message", "成功从URL导入 " + count + " 条记录"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ok", false, "error", "URL导入失败: " + e.getMessage()));
        }
    }

    private String extractJsonFromResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) return "[]";

        Matcher m = JSON_BLOCK.matcher(rawResponse);
        if (m.find()) {
            return m.group(1).trim();
        }

        int start = rawResponse.indexOf('[');
        int end = rawResponse.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return rawResponse.substring(start, end + 1);
        }

        int startObj = rawResponse.indexOf('{');
        int endObj = rawResponse.lastIndexOf('}');
        if (startObj >= 0 && endObj > startObj) {
            return "[" + rawResponse.substring(startObj, endObj + 1) + "]";
        }

        return "[]";
    }

    private String buildExportPrompt(List<Map<String, Object>> records, DataSet ds,
                                      String dir, String sampleData, String customPrompt) {
        StringBuilder sb = new StringBuilder();

        if (customPrompt != null && !customPrompt.isBlank()) {
            sb.append(customPrompt).append("\n\n");
        } else {
            sb.append("请将以下数据转换为业务数据格式。\n\n");
        }

        sb.append("目标目录: ").append(dir).append("/data/\n\n");

        if (!sampleData.isEmpty()) {
            sb.append("现有业务数据格式样例（请保持一致）：\n");
            sb.append("```\n").append(sampleData).append("\n```\n\n");
        }

        sb.append("待导出数据（共").append(records.size()).append("条）：\n");
        try {
            String recordsJson = mapper.writeValueAsString(records);
            sb.append("```json\n");
            sb.append(recordsJson, 0, Math.min(recordsJson.length(), 8000));
            if (recordsJson.length() > 8000) sb.append("...");
            sb.append("\n```\n\n");
        } catch (Exception e) {
            sb.append("（数据序列化失败）\n\n");
        }

        sb.append("要求：\n");
        sb.append("1. 参考现有业务数据格式，将待导出数据转换为相同格式\n");
        sb.append("2. 直接输出JSON数组，用 ```json 包裹\n");
        sb.append("3. 不要输出其他说明文字\n");

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeWithAi(List<Map<String, Object>> records, DataSet ds) throws Exception {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请将以下数据按Schema定义进行标准化处理。\n\n");

        if (ds.getSchema() != null && ds.getSchema().getFields() != null && !ds.getSchema().getFields().isEmpty()) {
            prompt.append("目标Schema：\n");
            for (DataField field : ds.getSchema().getFields()) {
                prompt.append("- ").append(field.getName());
                if (field.getDisplayName() != null) prompt.append("（").append(field.getDisplayName()).append("）");
                if (field.getType() != null) prompt.append(" [").append(field.getType()).append("]");
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("处理要求：\n");
        prompt.append("1. 将原始字段名映射到Schema字段名（如\"阅读量\"→views，\"点赞数\"→likes）\n");
        prompt.append("2. 统一日期格式为 yyyy-MM-dd\n");
        prompt.append("3. 数值字段转为数字类型\n");
        prompt.append("4. 空值用空字符串或null填充\n");
        prompt.append("5. 保留所有有意义的数据，不要丢失字段\n\n");

        prompt.append("原始数据（共").append(records.size()).append("条）：\n");
        try {
            String recordsJson = mapper.writeValueAsString(records);
            prompt.append("```json\n");
            prompt.append(recordsJson, 0, Math.min(recordsJson.length(), 8000));
            if (recordsJson.length() > 8000) prompt.append("...");
            prompt.append("\n```\n\n");
        } catch (Exception e) {
            prompt.append("（数据序列化失败）\n\n");
        }

        prompt.append("直接输出标准化后的JSON数组，用 ```json 包裹，不要输出其他说明文字。");

        String rawResponse = doAiChat("你是一个数据标准化助手。严格按照要求的格式输出JSON数组。", prompt.toString());
        String jsonData = extractJsonFromResponse(rawResponse);

        Object parsed = mapper.readValue(jsonData, Object.class);
        List<Map<String, Object>> result = new ArrayList<>();
        if (parsed instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> record = new HashMap<>();
                    map.forEach((k, v) -> record.put(String.valueOf(k), v));
                    result.add(record);
                }
            }
        }

        log.info("AI normalized {} records to {} records", records.size(), result.size());
        return result.isEmpty() ? records : result;
    }
}