package cn.qihang.ai.assistant.util;

import cn.qihang.ai.assistant.security.common.SecurityUtils;

import java.util.Map;

public class DataMaskUtil {

    private DataMaskUtil() {}

    public static String mask(String value, int prefixVisible, int suffixVisible) {
        if (value == null || SecurityUtils.isLoggedIn()) {
            return value;
        }
        if (value.length() <= prefixVisible + suffixVisible) {
            return value;
        }
        String prefix = value.substring(0, Math.min(prefixVisible, value.length()));
        String suffix = value.substring(Math.max(0, value.length() - suffixVisible));
        int maskLen = value.length() - prefixVisible - suffixVisible;
        return prefix + "*".repeat(Math.max(0, maskLen)) + suffix;
    }

    public static String mask(String value) {
        return mask(value, 0, 0);
    }

    public static void apply(Map<String, Object> map, String... sensitiveKeys) {
        if (SecurityUtils.isLoggedIn()) return;
        for (String key : sensitiveKeys) {
            if (map.containsKey(key) && map.get(key) instanceof String) {
                map.put(key, mask((String) map.get(key)));
            }
        }
    }
}
