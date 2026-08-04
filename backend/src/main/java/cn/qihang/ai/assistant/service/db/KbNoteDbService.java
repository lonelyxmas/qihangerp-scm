package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.qihang.ai.assistant.entity.KbNoteEntity;

import java.util.List;

public interface KbNoteDbService extends IService<KbNoteEntity> {
    List<KbNoteEntity> listByKbId(Long kbId);
    List<KbNoteEntity> listByKbIdWithoutContent(Long kbId);
    KbNoteEntity getByKbIdAndPath(Long kbId, String path);
    KbNoteEntity getContentSnippet(Long noteId, int maxChars);
    void deleteByKbIdAndPath(Long kbId, String path);
    void deleteByKbId(Long kbId);
}