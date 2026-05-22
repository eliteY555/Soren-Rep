package com.voiceassistant.llm.config;

import com.voiceassistant.llm.factory.ModelFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfiguration {

    @Bean
    public ModelFactory modelFactory() {
        return new ModelFactory();
    }
}
