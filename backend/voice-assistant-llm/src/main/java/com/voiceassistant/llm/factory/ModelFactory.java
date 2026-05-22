package com.voiceassistant.llm.factory;

import com.voiceassistant.repo.mysql.entity.ProviderConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ModelFactory {

    private final Map<Long, ChatLanguageModel> chatModelCache = new ConcurrentHashMap<>();
    private final Map<Long, StreamingChatLanguageModel> streamModelCache = new ConcurrentHashMap<>();

    public ChatLanguageModel getOrCreateChatModel(ProviderConfig config, String decryptedApiKey) {
        return chatModelCache.computeIfAbsent(config.getId(),
                id -> buildChatModel(config, decryptedApiKey));
    }

    public StreamingChatLanguageModel getOrCreateStreamingModel(ProviderConfig config, String decryptedApiKey) {
        return streamModelCache.computeIfAbsent(config.getId(),
                id -> buildStreamingModel(config, decryptedApiKey));
    }

    public void evict(Long providerId) {
        chatModelCache.remove(providerId);
        streamModelCache.remove(providerId);
        log.info("Evicted cached models for providerId={}", providerId);
    }

    private ChatLanguageModel buildChatModel(ProviderConfig config, String decryptedApiKey) {
        log.info("Building ChatLanguageModel for provider: {} model: {}", config.getProvider(), config.getModelName());
        return OpenAiChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(decryptedApiKey)
                .modelName(config.getModelName())
                .timeout(Duration.ofSeconds(60))
                .maxRetries(2)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    private StreamingChatLanguageModel buildStreamingModel(ProviderConfig config, String decryptedApiKey) {
        log.info("Building StreamingChatLanguageModel for provider: {} model: {}", config.getProvider(), config.getModelName());
        return OpenAiStreamingChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(decryptedApiKey)
                .modelName(config.getModelName())
                .timeout(Duration.ofSeconds(120))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
