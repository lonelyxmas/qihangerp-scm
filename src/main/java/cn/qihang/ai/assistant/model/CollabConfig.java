package cn.qihang.ai.assistant.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollabConfig {
    private String assignedToField;
    private Boolean notifyAssigneeOnCreate;
    private Boolean notifyOnUpdate;
    private String feishuWebhookUrl;

    public String getAssignedToField() { return assignedToField; }
    public void setAssignedToField(String assignedToField) { this.assignedToField = assignedToField; }
    public Boolean getNotifyAssigneeOnCreate() { return notifyAssigneeOnCreate; }
    public void setNotifyAssigneeOnCreate(Boolean notifyAssigneeOnCreate) { this.notifyAssigneeOnCreate = notifyAssigneeOnCreate; }
    public Boolean getNotifyOnUpdate() { return notifyOnUpdate; }
    public void setNotifyOnUpdate(Boolean notifyOnUpdate) { this.notifyOnUpdate = notifyOnUpdate; }
    public String getFeishuWebhookUrl() { return feishuWebhookUrl; }
    public void setFeishuWebhookUrl(String feishuWebhookUrl) { this.feishuWebhookUrl = feishuWebhookUrl; }
}
