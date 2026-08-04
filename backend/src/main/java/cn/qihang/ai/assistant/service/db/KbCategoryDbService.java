package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.qihang.ai.assistant.entity.KbCategoryEntity;
import java.util.List;

public interface KbCategoryDbService extends IService<KbCategoryEntity> {
    List<KbCategoryEntity> listByKbId(Long kbId);
}
