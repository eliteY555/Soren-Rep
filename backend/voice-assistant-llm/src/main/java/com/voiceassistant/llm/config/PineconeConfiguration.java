package com.voiceassistant.llm.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    void validate() {
        if (pineconeApiKey.isBlank() || pineconeApiKey.contains("your-pinecone")) {
            log.error("PINECONE_API_KEY is not set! Set the environment variable or edit application.yml");
        } else {
            log.info("Pinecone API key configured (prefix: {})", pineconeApiKey.substring(0, Math.min(8, pineconeApiKey.length())) + "...");
        }
        if (embeddingApiKey.isBlank() || embeddingApiKey.contains("your-")) {
            log.error("QWEN_KEY (embedding.api-key) is not set! Set the environment variable or edit application.yml");
        } else {
            log.info("Embedding API key configured (prefix: {})", embeddingApiKey.substring(0, Math.min(8, embeddingApiKey.length())) + "...");
        }
    }

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
