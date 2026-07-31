package cn.qihang.ai.assistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskData {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskItem {
        public Long id;
        public String title;
        public String description;
        public String status;
        public String priority;
        public String createdAt;
        public String updatedAt;
        public String dueDate;
        public String action;
        public String actionPrompt;
        public String lastReminded;
        public String scheduledStart;
        public Long createdBy;
        public Long kbId;
        public String scheduleType;
        public String cycleType;
        public String cycleValue;
        public String cycleTime;
        public String cycleEnd;
        public String lastCycleRun;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Root {
        public Map<String, Object> meta;
        public List<TaskItem> tasks;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskExecution {
        public String executionId;
        public Long taskId;
        public String taskTitle;
        public String status;
        public String triggerType;
        public String triggeredBy;
        public String startTime;
        public String endTime;
        public String logText;
        public String resultText;
        public String errorMessage;
        public String createdAt;
    }
}
