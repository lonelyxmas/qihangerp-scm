package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.qihang.ai.assistant.entity.SystemLogEntity;
import cn.qihang.ai.assistant.mapper.SystemLogMapper;
import org.springframework.stereotype.Service;

@Service
public class SystemLogDbServiceImpl extends ServiceImpl<SystemLogMapper, SystemLogEntity> implements SystemLogDbService {
}
