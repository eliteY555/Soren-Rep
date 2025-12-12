package com.me.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.me.utils.CustomDateDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Jackson全局配置类
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 主要的ObjectMapper，用于Spring MVC的请求响应
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 日期格式化
        objectMapper.setDateFormat(new SimpleDateFormat(DATE_FORMAT));
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        
        // 注册自定义日期反序列化器
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Date.class, new CustomDateDeserializer());
        objectMapper.registerModule(module);
        
        // 配置序列化特性
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
        
        return objectMapper;
    }

    /**
     * 专门用于LangChain4j的ObjectMapper
     * 配置更宽松，以适应LangChain4j的特殊需求
     */
    @Bean("langChainObjectMapper")
    public ObjectMapper langChainObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 日期格式化
        objectMapper.setDateFormat(new SimpleDateFormat(DATE_FORMAT));
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        
        // 注册自定义日期反序列化器
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Date.class, new CustomDateDeserializer());
        objectMapper.registerModule(module);
        
        // 配置序列化特性 - 更宽松的配置
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        objectMapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        objectMapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
        
        return objectMapper;
    }
} 