package cn.qihang.ai.assistant.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollabConfig {
    private Boolean feishuNotify;

    public Boolean getFeishuNotify() { return feishuNotify; }
    public void setFeishuNotify(Boolean feishuNotify) { this.feishuNotify = feishuNotify; }
}