package com.voiceassistant.llm.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class PineconeConfiguration {

    @Value("${pinecone.api-key}")
    private String pineconeApiKey;

    @Value("${pinecone.index}")
    private String pineconeIndex;

    @Value("${embedding.api-key}")
    private String embeddingApiKey;

    @Value("${embedding.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String embeddingBaseUrl;

    @Value("${embedding.model-name:text-embedding-v3}")
    private String embeddingModelName;

    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("Initializing embedding model: {} via {}", embeddingModelName, embeddingBaseUrl);
        return OpenAiEmbeddingModel.builder()
                .baseUrl(embeddingBaseUrl)
                .apiKey(embeddingApiKey)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(2)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public PineconeClient pineconeClient() {
        log.info("Initializing Pinecone client: index={}", pineconeIndex);
        return new PineconeClient(pineconeApiKey, pineconeIndex);
    }
}
