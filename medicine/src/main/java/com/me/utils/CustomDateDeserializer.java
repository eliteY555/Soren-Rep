package com.me.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 自定义日期反序列化器
 * 用于处理各种格式的日期字符串和对象
 */
public class CustomDateDeserializer extends JsonDeserializer<Date> {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String[] DATE_FORMATS = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd"
    };

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String dateStr = p.getText();
        
        // 处理空值
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        // 如果是数字（时间戳），直接转换
        if (dateStr.matches("\\d+")) {
            try {
                long timestamp = Long.parseLong(dateStr);
                return new Date(timestamp);
            } catch (NumberFormatException e) {
                // 忽略错误，继续尝试其他格式
            }
        }
        
        // 尝试不同的日期格式
        for (String format : DATE_FORMATS) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(format);
                return dateFormat.parse(dateStr);
            } catch (ParseException e) {
                // 忽略错误，尝试下一个格式
            }
        }
        
        // 处理JSON对象格式的日期
        try {
            JsonNode node = p.getCodec().readTree(p);
            if (node.isObject()) {
                // 尝试获取fastTime字段（Java Date对象序列化后可能包含此字段）
                if (node.has("fastTime")) {
                    long timestamp = node.get("fastTime").asLong();
                    return new Date(timestamp);
                }
                // 尝试获取time字段
                else if (node.has("time")) {
                    long timestamp = node.get("time").asLong();
                    return new Date(timestamp);
                }
                // 尝试获取timestamp字段
                else if (node.has("timestamp")) {
                    long timestamp = node.get("timestamp").asLong();
                    return new Date(timestamp);
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        
        // 如果所有尝试都失败，返回当前时间
        return new Date();
    }
} 