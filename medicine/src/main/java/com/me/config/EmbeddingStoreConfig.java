package com.me.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pinecone 向量数据库（可选）
 * 仅当 BOTH pinecone.api-key 和 dashscope EmbeddingModel Bean 都可用时才激活。
 * 开发环境未设置 PINECONE_KEY 或 QWEN_KEY 时自动跳过，AI 对话仍可正常使用。
 */
@Configuration
public class EmbeddingStoreConfig {

    @Value("${pinecone.api-key:#{null}}")
    private String pineconeApiKey;

    @Bean
    @ConditionalOnProperty(name = "pinecone.api-key")
    @ConditionalOnBean(EmbeddingModel.class)
    public EmbeddingStore<TextSegment> pineconeEmbeddingStore(@Autowired EmbeddingModel embeddingModel) {
        return PineconeEmbeddingStore.builder()
                .apiKey(pineconeApiKey)
                .index("medicine-index")
                .nameSpace("medicine-namespace1")
                .createIndex(PineconeServerlessIndexConfig.builder()
                        .cloud("AWS")
                        .region("us-east-1")
                        .dimension(embeddingModel.dimension())
                        .build())
                .build();
    }
}
