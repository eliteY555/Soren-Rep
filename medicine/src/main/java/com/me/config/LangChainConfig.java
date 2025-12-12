package com.me.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain配置类
 * 用于配置LangChain的JSON处理
 */
@Configuration
public class LangChainConfig {

    /**
     * 为LangChain提供专用的ObjectMapper
     * 注意：这里使用了langChainObjectMapper，而不是默认的objectMapper
     */
    @Bean(name = "langChainInternalMapper")
    public ObjectMapper langChainInternalMapper(@Qualifier("langChainObjectMapper") ObjectMapper objectMapper) {
        return objectMapper;
    }
} 