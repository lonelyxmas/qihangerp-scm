package cn.qihang.ai.assistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import cn.qihang.ai.assistant.config.AppConfig;
import cn.qihang.ai.assistant.model.Config;
import cn.qihang.ai.assistant.model.LogEntry;
import cn.qihang.ai.assistant.util.FileUtil;
import cn.qihang.ai.assistant.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);
    private static final int MAX_LOG = 100;

    private final AppConfig appConfig;
    private final ConfigService configService;
    private final TypeReference<List<LogEntry>> logType = new TypeReference<List<LogEntry>>() {};

    public LogService(AppConfig appConfig, ConfigService configService) {
        this.appConfig = appConfig;
        this.configService = configService;
    }

    private Path getLogFile() {
        return appConfig.getConfigDirPath().resolve("assistant_log.json");
    }

    public List<LogEntry> load() {
        return FileUtil.readJson(getLogFile(), logType, new ArrayList<>());
    }

    public synchronized void add(String action, String status, String detail) {
        List<LogEntry> logs = load();
        logs.add(0, new LogEntry(TimeUtil.nowStr(), action, status, detail));
        if (logs.size() > MAX_LOG) logs = logs.subList(0, MAX_LOG);
        FileUtil.writeJson(getLogFile(), logs);
    }

    public void add(String action, String status) {
        add(action, status, "");
    }
}
