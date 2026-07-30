package cn.qihang.ai.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.qihang.ai.assistant.entity.KbNoteEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface KbNoteMapper extends BaseMapper<KbNoteEntity> {

    @Select("SELECT id, kb_id, path, name, is_dir, LEFT(content, #{maxChars}) AS content, file_type, file_size, tags, status, created_by, original_file, category_id, created_at, updated_at, content_hash, indexed_at FROM kb_notes WHERE id = #{id}")
    KbNoteEntity getContentSnippet(@Param("id") Long id, @Param("maxChars") int maxChars);
}