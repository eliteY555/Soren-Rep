package com.me.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.me.utils.CustomDateDeserializer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

/**
 * LangChain4j的JSON配置类
 * 不再使用反射方式修改LangChain内部的JSON处理器
 * 而是通过全局配置Jackson来解决问题
 */
@Configuration
public class LangChainJsonConfig {

    private static final Logger logger = LoggerFactory.getLogger(LangChainJsonConfig.class);

    @Autowired
    @Qualifier("langChainObjectMapper")
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try {
            // 配置ObjectMapper
            SimpleModule module = new SimpleModule();
            module.addDeserializer(Date.class, new CustomDateDeserializer());
            objectMapper.registerModule(module);
            
            // 配置序列化特性
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
            
            logger.info("LangChain4j的JSON配置已完成");
        } catch (Exception e) {
            logger.error("配置LangChain4j的JSON处理失败: {}", e.getMessage(), e);
        }
    }
} 