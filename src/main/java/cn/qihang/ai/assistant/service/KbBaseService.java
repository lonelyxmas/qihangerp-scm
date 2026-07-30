package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.security.common.SecurityUtils;
import cn.qihang.ai.assistant.service.db.KbBaseDbService;
import cn.qihang.ai.assistant.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KbBaseService {

    private static final Logger log = LoggerFactory.getLogger(KbBaseService.class);

    private final KbBaseDbService kbBaseDbService;

    public KbBaseService(KbBaseDbService kbBaseDbService) {
        this.kbBaseDbService = kbBaseDbService;
    }

    public List<KbBaseEntity> getAll() {
        return kbBaseDbService.lambdaQuery()
                .eq(KbBaseEntity::getDeleted, 0)
                .orderByAsc(KbBaseEntity::getSortOrder)
                .list();
    }

    public KbBaseEntity getById(Long id) {
        KbBaseEntity e = kbBaseDbService.getById(id);
        if (e != null && e.getDeleted() != null && e.getDeleted() == 1) return null;
        return e;
    }

    public KbBaseEntity getFirst() {
        return kbBaseDbService.lambdaQuery()
                .eq(KbBaseEntity::getDeleted, 0)
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
        if (body.containsKey("visibility")) {
            e.setVisibility(str(body.get("visibility")));
        }

        if (isNew) {
            int maxOrder = kbBaseDbService.lambdaQuery()
                    .eq(KbBaseEntity::getDeleted, 0)
                    .orderByDesc(KbBaseEntity::getSortOrder)
                    .list().stream()
                    .findFirst()
                    .map(k -> k.getSortOrder() + 1)
                    .orElse(0);
            e.setSortOrder(maxOrder);
            e.setLabels("{}");
            e.setDeleted(0);
            e.setCreatedAt(TimeUtil.nowStr());
            kbBaseDbService.save(e);
        } else {
            kbBaseDbService.updateById(e);
        }
    }

    public void delete(Long id) {
        KbBaseEntity e = kbBaseDbService.getById(id);
        if (e == null) return;

        e.setDeleted(1);
        kbBaseDbService.updateById(e);
    }

    public void setActive(Long id) {
    }

    public List<KbBaseEntity> getAccessibleKbs() {
        List<KbBaseEntity> all = getAll();
        if (SecurityUtils.isLoggedIn()) {
            return all;
        }
        return all.stream()
                .filter(kb -> "public".equals(kb.getVisibility()))
                .collect(Collectors.toList());
    }

    public boolean isKbAccessible(Long kbId) {
        KbBaseEntity kb = getById(kbId);
        if (kb == null) return false;
        if (SecurityUtils.isLoggedIn()) return true;
        return "public".equals(kb.getVisibility());
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