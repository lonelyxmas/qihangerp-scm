package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.KbEmbeddingEntity;
import cn.qihang.ai.assistant.service.db.KbEmbeddingDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NoteIndexService {

    private static final Logger log = LoggerFactory.getLogger(NoteIndexService.class);

    private static final float SIMILARITY_THRESHOLD = 0.3f;

    private final KbEmbeddingDbService noteEmbeddingDbService;
    private final OllamaEmbeddingService embeddingService;

    public NoteIndexService(KbEmbeddingDbService noteEmbeddingDbService,
                           OllamaEmbeddingService embeddingService) {
        this.noteEmbeddingDbService = noteEmbeddingDbService;
        this.embeddingService = embeddingService;
    }

    public boolean isAvailable() {
        return embeddingService.isAvailable();
    }

    public List<NoteSearchResult> search(Long kbId, String query, int limit) {
        if (!isAvailable()) {
            throw new IllegalStateException("Embedding 服务不可用");
        }

        float[] queryVector = embeddingService.embed(query);
        if (queryVector == null) {
            throw new RuntimeException("生成查询向量失败");
        }

        List<KbEmbeddingEntity> allEntities = noteEmbeddingDbService.lambdaQuery()
                .eq(KbEmbeddingEntity::getKbId, kbId)
                .list();

        if (allEntities.isEmpty()) {
            return List.of();
        }

        List<ScoredChunk> scored = new ArrayList<>();
        int checkedCount = 0;
        int matchedCount = 0;
        for (KbEmbeddingEntity entity : allEntities) {
            float[] vec = bytesToFloat(Base64.getDecoder().decode(entity.getEmbedding()));
            float score = cosineSimilarity(queryVector, vec);
            checkedCount++;
            if (score >= SIMILARITY_THRESHOLD) {
                matchedCount++;
                scored.add(new ScoredChunk(entity, score));
            }
        }

        scored.sort((a, b) -> Float.compare(b.score, a.score));

        Map<String, Integer> perFileCount = new HashMap<>();
        List<NoteSearchResult> results = new ArrayList<>();

        for (ScoredChunk chunk : scored) {
            int count = perFileCount.getOrDefault(chunk.entity.getFilePath(), 0);
            if (count >= 1) continue;

            perFileCount.put(chunk.entity.getFilePath(), count + 1);
            results.add(new NoteSearchResult(
                    chunk.entity.getFilePath(),
                    chunk.entity.getPathContext(),
                    chunk.entity.getContent(),
                    chunk.score,
                    chunk.entity.getChunkIndex(),
                    0
            ));

            if (results.size() >= limit) break;
        }

        return results;
    }

    public List<NoteSearchResult> hybridSearch(Long kbId, String query, int limit) {
        List<String> expandedQueries = expandQuery(query);

        Map<String, NoteSearchResult> resultMap = new HashMap<>();
        Map<String, Double> scores = new HashMap<>();

        List<NoteSearchResult> semanticResults = search(kbId, query, limit * 2);

        for (NoteSearchResult r : semanticResults) {
            String key = r.filePath();
            double score = r.score();

            String pathLower = r.pathContext() != null ? r.pathContext().toLowerCase() : "";
            boolean pathMatch = isPathMatchPath(pathLower, query.toLowerCase());

            if (pathMatch) {
                score = Math.max(score, 0.9);
            }

            scores.put(key, score);
            resultMap.put(key, r);
        }

        for (String q : expandedQueries) {
            if (q.equals(query)) continue;

            List<NoteSearchResult> keywordResults = keywordSearch(kbId, q, limit * 2);

            for (NoteSearchResult r : keywordResults) {
                String key = r.filePath();

                if (scores.containsKey(key)) {
                    String pathLower = r.pathContext() != null ? r.pathContext().toLowerCase() : "";
                    boolean pathMatch = isPathMatchPath(pathLower, q.toLowerCase());
                    double bonus = pathMatch ? 0.4 : 0.2;
                    double newScore = Math.min(1.0, scores.get(key) + bonus);
                    scores.put(key, newScore);
                } else {
                    String pathLower = r.pathContext() != null ? r.pathContext().toLowerCase() : "";
                    boolean pathMatch = isPathMatchPath(pathLower, q.toLowerCase());
                    double score = pathMatch ? 0.85 : 0.5;
                    scores.put(key, score);
                    resultMap.put(key, r);
                }
            }
        }

        return scores.entrySet().stream()
                .filter(e -> e.getValue() >= 0.3)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    NoteSearchResult original = resultMap.get(e.getKey());
                    if (original != null) {
                        return new NoteSearchResult(
                                original.filePath(),
                                original.pathContext(),
                                original.content(),
                                e.getValue().floatValue(),
                                original.chunkIndex(),
                                original.totalChunks()
                        );
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<NoteSearchResult> keywordSearch(Long kbId, String query, int limit) {
        List<KbEmbeddingEntity> allEntities = noteEmbeddingDbService.lambdaQuery()
                .eq(KbEmbeddingEntity::getKbId, kbId)
                .list();

        String queryLower = query.toLowerCase();
        List<NoteSearchResult> results = new ArrayList<>();

        for (KbEmbeddingEntity entity : allEntities) {
            String content = entity.getContent();
            String pathContext = entity.getPathContext();

            boolean contentMatch = content != null && content.toLowerCase().contains(queryLower);
            boolean pathMatch = pathContext != null && pathContext.toLowerCase().contains(queryLower);

            if (!contentMatch && !pathMatch) continue;

            float score = 0;
            if (contentMatch) {
                int matchCount = countOccurrences(content.toLowerCase(), queryLower);
                score = Math.min(1.0f, matchCount * 0.2f);
            }
            if (pathMatch) {
                score = Math.max(score, 0.5f);
            }

            results.add(new NoteSearchResult(
                    entity.getFilePath(),
                    pathContext,
                    content != null ? content.substring(0, Math.min(500, content.length())) : "",
                    score,
                    entity.getChunkIndex(),
                    0
            ));
        }

        return results.stream()
                .sorted((a, b) -> Float.compare(b.score(), a.score()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<String> expandQuery(String query) {
        List<String> queries = new ArrayList<>();
        queries.add(query);

        String[] words = query.split("[\\s,，、]+");
        if (words.length > 1) {
            for (String word : words) {
                if (word.length() >= 2) {
                    queries.add(word);
                }
            }
        }

        String[] suffixes = {"项目", "记录"};
        for (String suffix : suffixes) {
            if (!query.endsWith(suffix)) {
                queries.add(query + suffix);
            }
        }

        if (query.contains(" ")) {
            String[] parts = query.split("\\s+");
            if (parts.length == 2) {
                queries.add(parts[0] + parts[1]);
                queries.add(parts[1] + parts[0]);
            }
        }

        return queries.stream().distinct().collect(java.util.stream.Collectors.toList());
    }

    private boolean isPathMatchPath(String pathLower, String queryLower) {
        if (pathLower == null || queryLower == null) return false;

        if (pathLower.contains(queryLower)) return true;

        String[] queryWords = queryLower.split("[\\s,，、]+");
        if (queryWords.length <= 1) return false;

        for (String word : queryWords) {
            if (word.length() >= 2 && !pathLower.contains(word)) {
                return false;
            }
        }
        return true;
    }

    public void clearIndex(Long kbId) {
        noteEmbeddingDbService.deleteByKb(kbId);
    }

    public IndexStats getIndexStats(Long kbId) {
        int fileCount = noteEmbeddingDbService.countFilesByKb(kbId);
        int chunkCount = noteEmbeddingDbService.countByKb(kbId);
        return new IndexStats(fileCount, chunkCount);
    }

    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : (float) (dot / denom);
    }

    private byte[] floatToBytes(float[] values) {
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(values.length * 4);
        buf.order(java.nio.ByteOrder.nativeOrder());
        for (float v : values) buf.putFloat(v);
        return buf.array();
    }

    private float[] bytesToFloat(byte[] bytes) {
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(bytes);
        buf.order(java.nio.ByteOrder.nativeOrder());
        float[] result = new float[bytes.length / 4];
        for (int i = 0; i < result.length; i++) {
            result[i] = buf.getFloat();
        }
        return result;
    }

    private int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    public record NoteSearchResult(
            String filePath,
            String pathContext,
            String content,
            float score,
            int chunkIndex,
            int totalChunks
    ) {}

    private record ScoredChunk(KbEmbeddingEntity entity, float score) {}

    public record IndexStats(int fileCount, int chunkCount) {}
}