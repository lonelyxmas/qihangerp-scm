package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.KbEmbeddingEntity;
import cn.qihang.ai.assistant.entity.KbNoteEntity;
import cn.qihang.ai.assistant.service.db.KbEmbeddingDbService;
import cn.qihang.ai.assistant.service.db.KbNoteDbService;
import cn.qihang.ai.assistant.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class KbIndexingService {

    private static final Logger log = LoggerFactory.getLogger(KbIndexingService.class);

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;
    private static final int MAX_CONTENT_LENGTH = 50_000;

    private final KbNoteDbService noteDbService;
    private final KbEmbeddingDbService embeddingDbService;
    private final EmbeddingService embeddingService;

    private final AtomicInteger pendingCount = new AtomicInteger(0);
    private final AtomicInteger totalIndexed = new AtomicInteger(0);
    private volatile String lastIndexTime = "";
    private final ConcurrentHashMap<Long, ReindexProgress> reindexProgress = new ConcurrentHashMap<>();
    /** 笔记级锁，防止同一个 noteId 的 indexNote 并发写入重复数据 */
    private final ConcurrentHashMap<Long, Object> noteIndexLocks = new ConcurrentHashMap<>();

    public KbIndexingService(KbNoteDbService noteDbService,
                             KbEmbeddingDbService embeddingDbService,
                             EmbeddingService embeddingService) {
        this.noteDbService = noteDbService;
        this.embeddingDbService = embeddingDbService;
        this.embeddingService = embeddingService;
    }

    public boolean isAvailable() {
        return embeddingService.isAvailable();
    }

    public IndexingStatus getStatus() {
        return new IndexingStatus(
                isAvailable(),
                pendingCount.get(),
                totalIndexed.get(),
                lastIndexTime
        );
    }

    public IndexingStatus getKbStatus(Long kbId) {
        ReindexProgress p = reindexProgress.get(kbId);
        boolean running = p != null && p.total > 0;
        int progress = running ? (int) ((double) p.done / p.total * 100) : 0;
        return new IndexingStatus(
                isAvailable(),
                pendingCount.get(),
                running ? p.done : totalIndexed.get(),
                lastIndexTime,
                running,
                progress
        );
    }

    public record IndexingStatus(
            boolean available,
            int pendingCount,
            int totalIndexed,
            String lastIndexTime,
            boolean running,
            int progress
    ) {
        public IndexingStatus(boolean available, int pendingCount, int totalIndexed, String lastIndexTime) {
            this(available, pendingCount, totalIndexed, lastIndexTime, false, 0);
        }
    }

    private record ReindexProgress(int total, int done) {}

    public void indexNote(Long kbId, Long noteId) {
        pendingCount.incrementAndGet();
        try {
            if (!isAvailable()) {
                log.warn("Embedding 服务不可用，跳过索引 noteId={}", noteId);
                return;
            }

            KbNoteEntity note = noteDbService.getContentSnippet(noteId, MAX_CONTENT_LENGTH);
            if (note == null || note.getIsDir() == 1) {
                log.debug("跳过索引 noteId={}: 笔记不存在或是目录", noteId);
                return;
            }
            String content = note.getContent();
            if (content == null || content.isBlank()) {
                log.debug("跳过索引 noteId={}: 内容为空", noteId);
                return;
            }

            // 清理 PDF 转码残留的 Base64 签名等垃圾文本
            content = sanitizeContent(content);
            if (content.isBlank()) {
                log.debug("跳过索引 noteId={}: 清理后内容为空", noteId);
                return;
            }

            int originalLen = content.length();

            if (originalLen > MAX_CONTENT_LENGTH) {
                log.warn("正文长度({})超过限制({})，截断处理 noteId={}", originalLen, MAX_CONTENT_LENGTH, noteId);
                content = content.substring(0, MAX_CONTENT_LENGTH);
                originalLen = MAX_CONTENT_LENGTH;
            }

            String contentHash = md5(content);

            // 笔记级互斥锁：防止同一个 noteId 并发写入导致重复数据
            Object lock = noteIndexLocks.computeIfAbsent(noteId, k -> new Object());
            synchronized (lock) {
                // 在锁内重新查 content_hash，避免并发时读到缓存旧值
                KbNoteEntity fresh = noteDbService.getContentSnippet(noteId, MAX_CONTENT_LENGTH);
                if (fresh != null && contentHash.equals(fresh.getContentHash())) {
                    log.debug("跳过索引 noteId={}: 内容未变化", noteId);
                    return;
                }

                // 先清空旧 embedding，再写入新的
                embeddingDbService.deleteByNoteId(noteId);

                String now = TimeUtil.nowStr();
                AtomicInteger chunkCount = new AtomicInteger(0);
                AtomicInteger successCount = new AtomicInteger(0);

                forEachChunk(content, (Chunk chunk) -> {
                    chunkCount.incrementAndGet();
                    log.debug("生成 embedding chunk={} noteId={}", chunk.index, noteId);
                    float[] vec = embeddingService.embed(chunk.text);
                    if (vec == null) {
                        log.warn("embedding 生成失败 chunk={} noteId={}", chunk.index, noteId);
                        return;
                    }

                    KbEmbeddingEntity entity = new KbEmbeddingEntity();
                    entity.setKbId(kbId);
                    entity.setNoteId(noteId);
                    entity.setFilePath(note.getPath());
                    entity.setChunkIndex(chunk.index);
                    entity.setChunkSize(chunk.text.length());
                    entity.setContent(chunk.text);
                    entity.setEmbedding(Base64.getEncoder().encodeToString(floatToBytes(vec)));
                    entity.setContentHash(contentHash);
                    entity.setCreatedAt(now);
                    entity.setUpdatedAt(now);
                    embeddingDbService.save(entity);
                    successCount.incrementAndGet();
                });

                // 回写索引状态到 kb_notes（只更新 hash 和索引时间，不覆盖 content）
                noteDbService.lambdaUpdate()
                        .eq(KbNoteEntity::getId, noteId)
                        .set(KbNoteEntity::getContentHash, contentHash)
                        .set(KbNoteEntity::getIndexedAt, now)
                        .update();

                int totalChunks = chunkCount.get();
                totalIndexed.incrementAndGet();
                lastIndexTime = now;
                log.info("已索引笔记 noteId={}, path={}, 原始长度={}, 分块={}, 成功={}",
                        noteId, note.getPath(), originalLen, totalChunks, successCount.get());
            }
        } finally {
            pendingCount.decrementAndGet();
        }
    }

    public void removeNoteIndex(Long noteId) {
        if (noteId == null) return;
        embeddingDbService.deleteByNoteId(noteId);
        // 清空笔记的索引状态
        noteDbService.lambdaUpdate()
                .eq(KbNoteEntity::getId, noteId)
                .set(KbNoteEntity::getContentHash, null)
                .set(KbNoteEntity::getIndexedAt, null)
                .update();
        log.info("已删除笔记索引 noteId={}", noteId);
    }

    @Async
    public void reindexKb(Long kbId) {
        log.info("开始全量重索引 kbId={}", kbId);
        if (!isAvailable()) {
            log.warn("Embedding 服务不可用，跳过全量重索引 kbId={}", kbId);
            return;
        }

        // 防止同一个知识库并发重索引
        ReindexProgress existing = reindexProgress.putIfAbsent(kbId, new ReindexProgress(0, 0));
        if (existing != null) {
            log.warn("知识库 kbId={} 正在重索引中，跳过重复请求 (progress={}/{})", kbId, existing.done(), existing.total());
            return;
        }

        try {
            embeddingDbService.deleteByKb(kbId);
            List<KbNoteEntity> notes = noteDbService.listByKbIdWithoutContent(kbId);
            int total = notes.size();
            int count = 0;
            int skipped = 0;
            reindexProgress.put(kbId, new ReindexProgress(total, count));

            for (KbNoteEntity note : notes) {
                if (note.getIsDir() == 1) { skipped++; continue; }
                try {
                    indexNote(kbId, note.getId());
                    count++;
                    reindexProgress.put(kbId, new ReindexProgress(total, count));
                    if (count % 10 == 0) {
                        log.info("重索引进度 kbId={}, {}/{} 笔记已处理", kbId, count, total - skipped);
                    }
                } catch (Exception e) {
                    log.error("重索引笔记异常 noteId={}, path={}, 跳过当前笔记继续", note.getId(), note.getPath(), e);
                    skipped++;
                }
            }

            log.info("全量重索引完成 kbId={}, 总笔记={}, 已索引={}, 跳过(目录)={}", kbId, total, count, skipped);
        } finally {
            reindexProgress.remove(kbId);
        }
    }

    // TODO 待 content_hash 功能验证通过后恢复
     @Scheduled(initialDelay = 600_000, fixedDelay = 300_000)
    public void scheduledIncrementalIndex() {
        if (!isAvailable()) {
            log.debug("Embedding 服务不可用，跳过定时增量索引");
            return;
        }
        log.info("开始定时增量索引...");
        List<KbNoteEntity> allNotes = noteDbService.lambdaQuery()
                .select(KbNoteEntity.class, info -> !info.getColumn().equals("content"))
                .list();
        int total = allNotes.size();
        int indexed = 0;
        int skipped = 0;
        for (KbNoteEntity note : allNotes) {
            if (note.getIsDir() == 1) { skipped++; continue; }
            KbNoteEntity withContent = noteDbService.getContentSnippet(note.getId(), MAX_CONTENT_LENGTH);
            String content = withContent != null ? withContent.getContent() : null;
            if (content == null || content.isBlank()) { skipped++; continue; }
            String hash = md5(content);
            // 使用 kb_notes.content_hash 做变更检测（getContentSnippet 已包含该字段），避免查 embedding 表
            boolean changed = !hash.equals(withContent.getContentHash());
            if (changed) {
                log.debug("增量索引发现变化笔记 noteId={}, path={}", note.getId(), note.getPath());
                indexNote(note.getKbId(), note.getId());
                indexed++;
            }
        }
        log.info("定时增量索引完成, 总笔记={}, 已更新={}, 跳过(目录/空/未变化)={}", total, indexed, skipped + (total - indexed - skipped));
    }

    /**
     * 流式处理文本分块，每分一块立即回调 consumer，避免所有 chunk 同时占用堆内存。
     */
    private void forEachChunk(String text, Consumer<Chunk> consumer) {
        int start = 0;
        int index = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int breakPoint = findSentenceBoundary(text, end);
                if (breakPoint > start) end = breakPoint;
            }
            consumer.accept(new Chunk(index++, text.substring(start, end)));
            if (end == text.length()) break;
            start = Math.max(end - CHUNK_OVERLAP, 0);
        }
    }

    /**
     * 清理 PDF 转码残留的 Base64 签名、控制字符等无意义文本。
     */
    private String sanitizeContent(String content) {
        // 移除长度 >= 80 的连续 Base64 字符块（PDF 数字签名等）
        String cleaned = content.replaceAll("[A-Za-z0-9+/=]{80,}", "");
        // 移除孤立的控制字符（保留换行和制表符）
        cleaned = cleaned.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        return cleaned.trim();
    }

    private int findSentenceBoundary(String text, int from) {
        int searchEnd = Math.min(from + 100, text.length());
        for (int i = from; i < searchEnd; i++) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '\n' || c == '\r') {
                return i + 1;
            }
        }
        for (int i = from; i < searchEnd; i++) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == ';') {
                return i + 1;
            }
        }
        return from;
    }

    private String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(text.hashCode());
        }
    }

    private byte[] floatToBytes(float[] values) {
        ByteBuffer buf = ByteBuffer.allocate(values.length * 4);
        buf.order(ByteOrder.nativeOrder());
        for (float v : values) buf.putFloat(v);
        return buf.array();
    }

    private record Chunk(int index, String text) {}
}