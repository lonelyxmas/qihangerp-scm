package cn.qihang.ai.assistant.controller.api;

import cn.qihang.ai.assistant.config.AppConfig;
import cn.qihang.ai.assistant.dto.ApiResult;
import cn.qihang.ai.assistant.dto.ColumnSettingsResult;
import cn.qihang.ai.assistant.model.Config;
import cn.qihang.ai.assistant.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/data")
public class DataSetApiController {

    private static final Logger log = LoggerFactory.getLogger(DataSetApiController.class);

    private final ConfigService configService;
    private final AppConfig appConfig;

    public DataSetApiController(ConfigService configService, AppConfig appConfig) {
        this.configService = configService;
        this.appConfig = appConfig;
    }

    @GetMapping(value = "/column-settings", produces = "application/json")
    public ColumnSettingsResult getColumnSettings(
            @RequestParam(required = false, defaultValue = "customer") String type) {
        Config config = configService.load();
        Map<String, Map<String, List<String>>> allSettings = config.getColumnSettings();
        if (allSettings == null) {
            allSettings = new HashMap<>();
        }
        Map<String, List<String>> typeSettings = allSettings.getOrDefault(type, new HashMap<>());
        return ColumnSettingsResult.success(type, typeSettings);
    }

    @PostMapping(value = "/column-settings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Void> saveColumnSettings(
            @RequestParam(required = false, defaultValue = "customer") String type,
            @RequestBody Map<String, List<String>> settings) {
        try {
            Config config = configService.load();
            Map<String, Map<String, List<String>>> allSettings = config.getColumnSettings();
            if (allSettings == null) {
                allSettings = new HashMap<>();
            }
            allSettings.put(type, settings);
            config.setColumnSettings(allSettings);
            configService.save(config);
            return ApiResult.success();
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }
}
