package cn.qihang.ai.assistant.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class IndexScannerService {
    private static final Logger log = LoggerFactory.getLogger(IndexScannerService.class);
    private final NoteIndexService noteIndexService;
    private final KbBaseService kbService;
    public IndexScannerService(NoteIndexService ns, KbBaseService kb) {
        this.noteIndexService = ns; this.kbService = kb;
    }

    public boolean isEmbeddingAvailable() {
        return noteIndexService.isAvailable();
    }
    public ScanResult scanKb(Long kbId) {
        log.info("[IndexScanner] 开始增量扫描, kbId={}", kbId);
        if (!noteIndexService.isAvailable()) return new ScanResult("Embedding 不可用");
        String notesDir = kbService.getNotesDirById(kbId);
        if (notesDir == null || notesDir.isBlank()) return new ScanResult("路径未配置");
        Path baseDir = Paths.get(notesDir);
        if (!Files.isDirectory(baseDir)) return new ScanResult("路径不存在");
        Set<String> ignoredDirs = noteIndexService.getIgnoredDirs(kbId);
        Set<String> ignoredFiles = noteIndexService.getIgnoredFiles(kbId);
        ScanResult result = new ScanResult();
        result.kbId = kbId;
        result.scanTime = java.time.LocalDateTime.now().toString();
        try (Stream<Path> walk = Files.walk(baseDir, 10)) {
            List<Path> files = walk.filter(Files::isRegularFile)
                    .filter(p -> noteIndexService.shouldIndex(p, baseDir, ignoredDirs, ignoredFiles))
                    .sorted().collect(Collectors.toList());
            for (Path file : files) {
                try {
                    boolean indexed = noteIndexService.indexSingleFile(file, baseDir, kbId);
                    if (indexed) {
                        result.newFiles++;
                    } else {
                        result.skippedFiles++;
                    }
                    result.actualChanged++;
                } catch (Exception e) {
                    log.warn("[IndexScanner] 处理失败: {}", file, e);
                    result.errors++; result.errorDetails.add(file.getFileName().toString() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            result.errorDetails.add("扫描失败: " + e.getMessage());
            return result;
        }
        log.info("[IndexScanner] 完成: kbId={}, 新={}, 跳={}, 错={}",
                kbId, result.newFiles, result.skippedFiles, result.errors);
        return result;
    }
    public Map<Long, ScanResult> scanAllKbs() {
        var allKbs = kbService.getAll();
        Map<Long, ScanResult> results = new LinkedHashMap<>();
        for (var kb : allKbs) {
            try { results.put(kb.getId(), scanKb(kb.getId()));
            } catch (Exception e) {
                ScanResult r = new ScanResult("失败: " + e.getMessage());
                r.kbId = kb.getId(); results.put(kb.getId(), r);
            }
        }
        return results;
    }
    public static class ScanResult {
        public Long kbId; public String scanTime;
        public int newFiles, modifiedFiles, deletedFiles, skippedFiles, actualChanged, errors;
        public List<String> errorDetails; public String errorMessage; public boolean success;
        public ScanResult() {
            this.newFiles=0; this.modifiedFiles=0; this.deletedFiles=0;
            this.skippedFiles=0; this.actualChanged=0; this.errors=0;
            this.errorDetails=new ArrayList<>(); this.success=true;
        }
        public ScanResult(String msg) { this(); this.success=false; this.errorMessage=msg; }
        public int getTotalFiles() { return newFiles+modifiedFiles+deletedFiles+skippedFiles; }
    }
}