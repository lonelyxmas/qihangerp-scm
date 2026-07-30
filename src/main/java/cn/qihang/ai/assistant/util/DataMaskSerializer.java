package cn.qihang.ai.assistant.util;

import cn.qihang.ai.assistant.security.common.SecurityUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

public class DataMaskSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private String maskChar = "*";
    private int prefixVisible = 0;
    private int suffixVisible = 0;

    public DataMaskSerializer() {}

    public DataMaskSerializer(String maskChar, int prefixVisible, int suffixVisible) {
        this.maskChar = maskChar;
        this.prefixVisible = prefixVisible;
        this.suffixVisible = suffixVisible;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (SecurityUtils.isLoggedIn()) {
            gen.writeString(value);
            return;
        }
        gen.writeString(mask(value));
    }

    private String mask(String value) {
        if (value.length() <= prefixVisible + suffixVisible) {
            return value;
        }
        String prefix = value.substring(0, Math.min(prefixVisible, value.length()));
        String suffix = value.substring(Math.max(0, value.length() - suffixVisible));
        int maskLen = value.length() - prefixVisible - suffixVisible;
        return prefix + String.valueOf(maskChar).repeat(Math.max(0, maskLen)) + suffix;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        DataMask annotation = property.getAnnotation(DataMask.class);
        if (annotation != null) {
            return new DataMaskSerializer(annotation.maskChar(), annotation.prefixVisible(), annotation.suffixVisible());
        }
        return this;
    }
}
