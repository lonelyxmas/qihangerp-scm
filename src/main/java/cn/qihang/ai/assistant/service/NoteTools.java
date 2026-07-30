package cn.qihang.ai.assistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.qihang.ai.assistant.datacenter.DataSetService;
import cn.qihang.ai.assistant.datacenter.model.DataSet;
import cn.qihang.ai.assistant.service.db.ActivityLogDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class NoteTools {

    private static final Logger log = LoggerFactory.getLogger(NoteTools.class);
    private static final ThreadLocal<Long> CURRENT_KB_ID = new ThreadLocal<>();
    private static Consumer<String> STATUS_CALLBACK;

    private static final ObjectMapper mapper = new ObjectMapper();

    private final DataSetService dataSetService;
    private final ActivityLogDbService activityLogDbService;

    public NoteTools(DataSetService dataSetService, ActivityLogDbService activityLogDbService) {
        this.dataSetService = dataSetService;
        this.activityLogDbService = activityLogDbService;
    }

    public static void setCurrentKbId(Long kbId) {
        if (kbId != null) {
            CURRENT_KB_ID.set(kbId);
        } else {
            CURRENT_KB_ID.remove();
        }
    }

    public static Long getCurrentKbId() {
        return CURRENT_KB_ID.get();
    }

    public static void clearCurrentKbId() {
        CURRENT_KB_ID.remove();
    }

    public static void setStatusCallback(Consumer<String> callback) {
        STATUS_CALLBACK = callback;
    }

    public static void clearStatusCallback() {
        STATUS_CALLBACK = null;
    }

    private void reportStatus(String message) {
        Consumer<String> cb = STATUS_CALLBACK;
        if (cb != null) {
            cb.accept(message);
        } else {
            log.debug("[NoteTools] reportStatus 回调为 null（message={}）", message);
        }
    }

    @Tool(description = "同时保存笔记并添加数据集记录。当用户汇报工作、记录客户沟通、反馈问题时使用此工具"
        + "（而不是分别调用 writeFile 和 addRecord），确保详细内容记录到笔记，关键结构信息记录到数据集")
    public String logRecord(
            @ToolParam(description = "笔记文件路径，相对于笔记库根目录，如 \"客户/张三-2026-06-28.md\"") String notePath,
            @ToolParam(description = "笔记详细内容（Markdown格式）") String noteContent,
            @ToolParam(description = "数据集ID或名称，如 \"客户跟进\"、\"Bug追踪\"") String dataset,
            @ToolParam(description = "JSON格式的结构化数据，如 {\"公司\":\"张三\",\"阶段\":\"报价\"}") String jsonData) {
        reportStatus("📝 正在保存记录并更新数据集...");

        String datasetResult;
        try {
            DataSet ds = findDataset(dataset);
            if (ds == null) {
                datasetResult = "⚠️ 未找到数据集「" + dataset + "」";
            } else {
                Map<String, Object> record = mapper.readValue(jsonData, new TypeReference<Map<String, Object>>() {});
                record.put("更新时间", java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                int count = dataSetService.addRecords(ds.getId(), List.of(record), "ai_log");
                datasetResult = count > 0
                        ? "✅ 已添加到数据集「" + ds.getName() + "」"
                        : "⚠️ 数据集「" + ds.getName() + "」记录已存在（重复），未添加";
            }
        } catch (Exception e) {
            datasetResult = "⚠️ 数据集写入失败: " + e.getMessage();
            log.warn("[NoteTools] logRecord 写入数据集失败", e);
        }

        activityLogDbService.addLog("log_record", "AI 记录: " + notePath + " → " + dataset, "ai", null, "AI");
        reportStatus("✅ 完成");
        return "✅ 记录已保存到数据集: " + notePath + "\n" + datasetResult;
    }

    private DataSet findDataset(String identifier) {
        DataSet ds = dataSetService.getDataset(identifier);
        if (ds != null) return ds;
        List<DataSet> all = dataSetService.getAllDatasets();
        for (DataSet d : all) {
            if (d.getName().equalsIgnoreCase(identifier) || d.getName().contains(identifier)) {
                return d;
            }
        }
        return null;
    }
}