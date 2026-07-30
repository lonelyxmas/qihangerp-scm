package cn.qihang.ai.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("kb_notes")
public class KbNoteEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long kbId;
    private String path;
    private String name;
    private Integer isDir;
    private String content;
    private String createdAt;
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getKbId() { return kbId; }
    public void setKbId(Long kbId) { this.kbId = kbId; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getIsDir() { return isDir; }
    public void setIsDir(Integer isDir) { this.isDir = isDir; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}