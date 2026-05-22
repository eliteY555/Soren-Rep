package com.voiceassistant.web.config;

import com.voiceassistant.repo.mysql.entity.ProviderConfig;
import com.voiceassistant.repo.mysql.repository.ProviderConfigRepository;
import com.voiceassistant.service.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProviderConfigRepository providerRepo;

    @Override
    public void run(String... args) {
        if (providerRepo.count() == 0) {
            AesUtil aesUtil = new AesUtil("VoiceAsst2026!@#");
            ProviderConfig config = new ProviderConfig();
            config.setName("DeepSeek V4 Pro");
            config.setProvider("deepseek");
            config.setBaseUrl("https://api.deepseek.com/v1");
            config.setModelName("deepseek-chat");
            config.setApiKey(aesUtil.encrypt("your-deepseek-api-key-here"));
            config.setActive(true);
            providerRepo.save(config);
            log.info("Default provider 'DeepSeek V4 Pro' initialized. Please update the API key via /api/config/providers.");
        }
    }
}
