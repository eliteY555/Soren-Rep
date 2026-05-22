package com.voiceassistant.llm.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
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

    @Value("${pinecone.environment}")
    private String pineconeEnvironment;

    @Value("${pinecone.project-id}")
    private String pineconeProjectId;

    @Value("${pinecone.index}")
    private String pineconeIndex;

    @Value("${embedding.openai.api-key}")
    private String embeddingApiKey;

    @Value("${embedding.openai.model-name:text-embedding-3-small}")
    private String embeddingModelName;

    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("Initializing OpenAI embedding model: {}", embeddingModelName);
        return OpenAiEmbeddingModel.builder()
                .apiKey(embeddingApiKey)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(2)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        log.info("Initializing Pinecone embedding store: index={}, env={}", pineconeIndex, pineconeEnvironment);
        return PineconeEmbeddingStore.builder()
                .apiKey(pineconeApiKey)
                .environment(pineconeEnvironment)
                .projectId(pineconeProjectId)
                .index(pineconeIndex)
                .build();
    }
}
