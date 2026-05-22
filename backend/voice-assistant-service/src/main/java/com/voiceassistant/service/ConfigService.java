package com.voiceassistant.service;

import com.voiceassistant.common.dto.ProviderConfigDto;
import com.voiceassistant.repo.mysql.entity.ProviderConfig;
import com.voiceassistant.repo.mysql.repository.ProviderConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConfigService {

    private final ProviderConfigRepository providerRepo;
    private final AesUtil aesUtil;

    public ConfigService(ProviderConfigRepository providerRepo) {
        this.providerRepo = providerRepo;
        this.aesUtil = new AesUtil("VoiceAsst2026!@#");
    }

    public List<ProviderConfigDto> listProviders() {
        return providerRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProviderConfigDto getActiveProvider() {
        return providerRepo.findFirstByActiveTrue()
                .map(this::toDto)
                .orElse(null);
    }

    public ProviderConfigDto addProvider(ProviderConfigDto dto) {
        ProviderConfig entity = new ProviderConfig();
        entity.setName(dto.getName());
        entity.setProvider(dto.getProvider());
        entity.setBaseUrl(dto.getBaseUrl());
        entity.setModelName(dto.getModelName());
        entity.setApiKey(aesUtil.encrypt(dto.getApiKey()));
        entity.setActive(false);
        ProviderConfig saved = providerRepo.save(entity);
        return toDto(saved);
    }

    public ProviderConfigDto updateProvider(Long id, ProviderConfigDto dto) {
        ProviderConfig entity = providerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found: " + id));
        entity.setName(dto.getName());
        entity.setProvider(dto.getProvider());
        entity.setBaseUrl(dto.getBaseUrl());
        entity.setModelName(dto.getModelName());
        if (dto.getApiKey() != null && !dto.getApiKey().isBlank()) {
            entity.setApiKey(aesUtil.encrypt(dto.getApiKey()));
        }
        return toDto(providerRepo.save(entity));
    }

    public void deleteProvider(Long id) {
        providerRepo.deleteById(id);
    }

    public ProviderConfigDto activateProvider(Long id) {
        providerRepo.findAll().forEach(p -> {
            p.setActive(false);
            providerRepo.save(p);
        });
        ProviderConfig target = providerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found: " + id));
        target.setActive(true);
        return toDto(providerRepo.save(target));
    }

    public ProviderConfig findProviderEntity(Long id) {
        if (id != null) {
            return providerRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Provider not found: " + id));
        }
        return providerRepo.findFirstByActiveTrue()
                .orElseThrow(() -> new RuntimeException("No active provider configured"));
    }

    public String decryptApiKey(String encryptedKey) {
        return aesUtil.decrypt(encryptedKey);
    }

    private ProviderConfigDto toDto(ProviderConfig entity) {
        ProviderConfigDto dto = new ProviderConfigDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setProvider(entity.getProvider());
        dto.setBaseUrl(entity.getBaseUrl());
        dto.setModelName(entity.getModelName());
        dto.setActive(entity.getActive());
        // apiKey intentionally NOT exposed in DTO
        return dto;
    }
}
