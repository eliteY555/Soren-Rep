package com.voiceassistant.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ProviderConfigDto {
    private Long id;
    private String name;
    private String provider;       // openai, deepseek, qwen, etc.
    private String baseUrl;
    private String modelName;
    private Boolean active;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;         // 只写不入——查询时不返回
}
