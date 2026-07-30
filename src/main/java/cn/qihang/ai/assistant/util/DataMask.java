package cn.qihang.ai.assistant.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataMask {
    String maskChar() default "*";
    int prefixVisible() default 0;
    int suffixVisible() default 0;
}
