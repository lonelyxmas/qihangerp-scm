package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.service.db.KbBaseDbService;
import cn.qihang.ai.assistant.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class KbBaseService {

    private static final Logger log = LoggerFactory.getLogger(KbBaseService.class);

    private final KbBaseDbService kbBaseDbService;

    public KbBaseService(KbBaseDbService kbBaseDbService) {
        this.kbBaseDbService = kbBaseDbService;
    }

    public List<KbBaseEntity> getAll() {
        return kbBaseDbService.lambdaQuery()
                .orderByAsc(KbBaseEntity::getSortOrder)
                .list();
    }

    public KbBaseEntity getById(Long id) {
        return kbBaseDbService.getById(id);
    }

    public KbBaseEntity getFirst() {
        return kbBaseDbService.lambdaQuery()
                .orderByAsc(KbBaseEntity::getSortOrder)
                .last("LIMIT 1")
                .one();
    }

    public void save(Map<String, Object> body) {
        Object idRaw = body.get("id");
        Long id = idRaw != null ? Long.valueOf(idRaw.toString()) : null;

        KbBaseEntity e;
        boolean isNew = false;
        if (id != null && id > 0) {
            e = kbBaseDbService.getById(id);
            if (e == null) {
                e = new KbBaseEntity();
                e.setId(id);
            }
        } else {
            e = new KbBaseEntity();
            isNew = true;
        }

        if (body.containsKey("name")) e.setName(str(body.get("name")));
        if (body.containsKey("labels")) e.setLabels(str(body.get("labels")));
        if (body.containsKey("sortOrder")) {
            e.setSortOrder(Integer.valueOf(body.get("sortOrder").toString()));
        }
        if (body.containsKey("autoReport")) {
            Object v = body.get("autoReport");
            e.setAutoReport(Boolean.TRUE.equals(v) ? 1 : 0);
        }
        if (body.containsKey("feishuPush")) {
            Object v = body.get("feishuPush");
            e.setFeishuPush(Boolean.TRUE.equals(v) ? 1 : 0);
        }

        if (isNew) {
            int maxOrder = kbBaseDbService.lambdaQuery()
                    .orderByDesc(KbBaseEntity::getSortOrder)
                    .list().stream()
                    .findFirst()
                    .map(k -> k.getSortOrder() + 1)
                    .orElse(0);
            e.setSortOrder(maxOrder);
            e.setLabels("{}");
            e.setCreatedAt(TimeUtil.nowStr());
            kbBaseDbService.save(e);
        } else {
            kbBaseDbService.updateById(e);
        }
    }

    public void delete(Long id) {
        KbBaseEntity e = kbBaseDbService.getById(id);
        if (e == null) return;

        kbBaseDbService.removeById(id);
    }

    public void setActive(Long id) {
    }

    public void reorder(List<Long> ids) {
        int order = 0;
        for (Long id : ids) {
            kbBaseDbService.lambdaUpdate()
                    .eq(KbBaseEntity::getId, id)
                    .set(KbBaseEntity::getSortOrder, order++)
                    .update();
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}