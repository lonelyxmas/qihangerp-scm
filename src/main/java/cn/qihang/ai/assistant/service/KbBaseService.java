package cn.qihang.ai.assistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import cn.qihang.ai.assistant.config.AppConfig;
import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.service.db.KbBaseDbService;
import cn.qihang.ai.assistant.util.FileUtil;
import cn.qihang.ai.assistant.util.TimeUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class KbBaseService {

    private static final Logger log = LoggerFactory.getLogger(KbBaseService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final KbBaseDbService kbBaseDbService;
    private final AppConfig appConfig;

    public KbBaseService(KbBaseDbService kbBaseDbService, AppConfig appConfig) {
        this.kbBaseDbService = kbBaseDbService;
        this.appConfig = appConfig;
    }

    @PostConstruct
    public void migrateFromConfig() {
        try {
            long count = kbBaseDbService.count();
            if (count > 0) return;

            Map<String, Object> raw = FileUtil.readJson(appConfig.getConfigFile(), MAP_TYPE, Map.of());
            String notesDir = str(raw.get("notesDir"));
            if (notesDir == null || notesDir.isBlank()) {
                notesDir = str(raw.get("baseDir"));
            }
            if (notesDir == null || notesDir.isBlank()) return;

            KbBaseEntity kb = new KbBaseEntity();
            kb.setName("宸ヤ綔");
            kb.setNotesDir(notesDir);
            kb.setLabels("{\"tasks\":\"浠诲姟\",\"reminders\":\"鎻愰啋\",\"notes\":\"绗旇\",\"config\":\"閰嶇疆\"}");
            kb.setSortOrder(0);
            kb.setCreatedAt(TimeUtil.nowStr());
            kbBaseDbService.save(kb);
            log.info("宸蹭粠 config.json 杩佺Щ鐭ヨ瘑搴? name={}, notesDir={}", kb.getName(), kb.getNotesDir());
        } catch (Exception e) {
            log.warn("鐭ヨ瘑搴撹縼绉诲け璐ワ紙鍙兘鏄娆″惎鍔紝琛ㄥ皻鏈垱寤猴級: {}", e.getMessage());
        }
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
        if (body.containsKey("notesDir")) e.setNotesDir(str(body.get("notesDir")));
        if (body.containsKey("labels")) e.setLabels(str(body.get("labels")));
        if (body.containsKey("dirSettings")) e.setDirSettings(str(body.get("dirSettings")));
        if (body.containsKey("ignoreDirs")) e.setIgnoreDirs(str(body.get("ignoreDirs")));
        if (body.containsKey("ignoreFiles")) e.setIgnoreFiles(str(body.get("ignoreFiles")));
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

    public String getNotesDir() {
        KbBaseEntity first = getFirst();
        if (first != null) {
            return first.getNotesDir();
        }
        return "";
    }

    public String getNotesDirById(Long kbId) {
        if (kbId == null) return getNotesDir();
        KbBaseEntity kb = getById(kbId);
        if (kb != null) return kb.getNotesDir();
        return getNotesDir();
    }

    public Long getKbIdByNotesDir(String notesDir) {
        if (notesDir == null || notesDir.isBlank()) return null;
        return kbBaseDbService.lambdaQuery()
                .eq(KbBaseEntity::getNotesDir, notesDir)
                .last("LIMIT 1")
                .oneOpt()
                .map(KbBaseEntity::getId)
                .orElse(null);
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}