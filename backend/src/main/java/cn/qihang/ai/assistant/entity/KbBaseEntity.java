package cn.qihang.ai.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("kb_bases")
public class KbBaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String labels;
    private Integer sortOrder;
    private String createdAt;
    private Integer autoReport;
    private Integer feishuPush;
    private String visibility;
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLabels() { return labels; }
    public void setLabels(String labels) { this.labels = labels; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public Integer getAutoReport() { return autoReport; }
    public void setAutoReport(Integer autoReport) { this.autoReport = autoReport; }
    public Integer getFeishuPush() { return feishuPush; }
    public void setFeishuPush(Integer feishuPush) { this.feishuPush = feishuPush; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}