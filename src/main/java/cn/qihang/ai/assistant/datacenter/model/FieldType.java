package cn.qihang.ai.assistant.datacenter.model;

import java.util.Set;

/**
 * 数据集字段类型常量与工具方法。
 * 类型系统是审批条件、报表聚合、AI 查询共同依赖的基础设施：
 * number/money 支持数值比较与聚合，date 支持日期范围过滤，select 提供选项约束。
 */
public final class FieldType {

    public static final String TEXT = "text";
    public static final String TEXTAREA = "textarea";
    public static final String NUMBER = "number";
    public static final String MONEY = "money";
    public static final String DATE = "date";
    public static final String SELECT = "select";
    public static final String USER = "user";

    private static final Set<String> TYPES = Set.of(
            TEXT, TEXTAREA, NUMBER, MONEY, DATE, SELECT, USER);

    private FieldType() {}

    public static boolean isValid(String type) {
        return type != null && TYPES.contains(type);
    }

    /** 未知类型一律归一为 text，保证向后兼容 */
    public static String normalize(String type) {
        return isValid(type) ? type : TEXT;
    }

    public static boolean isNumeric(String type) {
        return NUMBER.equals(type) || MONEY.equals(type);
    }

    public static boolean isSelect(String type) {
        return SELECT.equals(type);
    }
}
