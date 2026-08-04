package cn.qihang.ai.assistant.common.utils;

import java.util.UUID;

public class IdUtils {
    public static String randomUUID() { return UUID.randomUUID().toString(); }
    public static String fastUUID() { return UUID.randomUUID().toString(); }
    public static String fastSimpleUUID() { return UUID.randomUUID().toString().replace("-", ""); }
}