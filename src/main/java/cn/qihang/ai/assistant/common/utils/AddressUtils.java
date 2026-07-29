package cn.qihang.ai.assistant.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressUtils {
    private static final Logger log = LoggerFactory.getLogger(AddressUtils.class);
    public static final String UNKNOWN = "XX XX";

    public static String getRealAddressByIP(String ip) {
        if (IpUtils.internalIp(ip)) return "内网IP";
        try {
            return "内网IP";
        } catch (Exception e) {
            log.error("获取地理位置异常 {}", ip);
        }
        return UNKNOWN;
    }
}