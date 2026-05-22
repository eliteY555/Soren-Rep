package com.voiceassistant.llm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class PineconeConfiguration {

    @Value("${pinecone.api-key}")
    private String pineconeApiKey;

    @Value("${pinecone.index}")
    private String pineconeIndex;

    @Bean
    public PineconeClient pineconeClient() {
        log.info("Initializing Pinecone client: index={}", pineconeIndex);
        return new PineconeClient(pineconeApiKey, pineconeIndex);
    }
}
