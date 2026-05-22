package com.voiceassistant.web.controller;

import com.voiceassistant.common.dto.ApiResponse;
import com.voiceassistant.common.dto.ProviderConfigDto;
import com.voiceassistant.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping("/providers")
    public ApiResponse<List<ProviderConfigDto>> listProviders() {
        return ApiResponse.success(configService.listProviders());
    }

    @PostMapping("/providers")
    public ApiResponse<ProviderConfigDto> addProvider(@RequestBody ProviderConfigDto dto) {
        return ApiResponse.success(configService.addProvider(dto));
    }

    @PutMapping("/providers/{id}")
    public ApiResponse<ProviderConfigDto> updateProvider(@PathVariable Long id,
                                                          @RequestBody ProviderConfigDto dto) {
        return ApiResponse.success(configService.updateProvider(id, dto));
    }

    @DeleteMapping("/providers/{id}")
    public ApiResponse<Void> deleteProvider(@PathVariable Long id) {
        configService.deleteProvider(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/providers/{id}/activate")
    public ApiResponse<ProviderConfigDto> activateProvider(@PathVariable Long id) {
        return ApiResponse.success(configService.activateProvider(id));
    }

    @GetMapping("/providers/active")
    public ApiResponse<ProviderConfigDto> getActiveProvider() {
        return ApiResponse.success(configService.getActiveProvider());
    }
}
