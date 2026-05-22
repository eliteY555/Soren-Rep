package com.voiceassistant.web.config;

import com.voiceassistant.repo.mysql.entity.ProviderConfig;
import com.voiceassistant.repo.mysql.repository.ProviderConfigRepository;
import com.voiceassistant.service.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final ProviderConfigRepository providerRepo;

    @Value("${initial-provider.name:DeepSeek V4 Pro}")
    private String defaultName;

    @Value("${initial-provider.provider:deepseek}")
    private String defaultProvider;

    @Value("${initial-provider.base-url:https://api.deepseek.com/v1}")
    private String defaultBaseUrl;

    @Value("${initial-provider.model-name:deepseek-chat}")
    private String defaultModelName;

    @Value("${initial-provider.api-key:}")
    private String defaultApiKey;

    public DataInitializer(ProviderConfigRepository providerRepo) {
        this.providerRepo = providerRepo;
    }

    @Override
    public void run(String... args) {
        if (providerRepo.count() == 0) {
            if (defaultApiKey.isBlank()) {
                log.warn("No initial provider API key configured. Set INITIAL_PROVIDER_API_KEY env variable "
                        + "or add via POST /api/config/providers after startup.");
                return;
            }

            AesUtil aesUtil = new AesUtil("VoiceAsst2026!@#");
            ProviderConfig config = new ProviderConfig();
            config.setName(defaultName);
            config.setProvider(defaultProvider);
            config.setBaseUrl(defaultBaseUrl);
            config.setModelName(defaultModelName);
            config.setApiKey(aesUtil.encrypt(defaultApiKey));
            config.setActive(true);
            providerRepo.save(config);
            log.info("Default provider '{}' initialized.", defaultName);
        }
    }
}
