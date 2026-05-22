package com.voiceassistant.common.dto;

import com.voiceassistant.common.enums.ChatMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatRequest {
    private String sessionId;        // null 则创建新会话
    @NotBlank
    private String content;
    @NotNull
    private ChatMode mode;
    private Long providerId;         // null 则使用默认提供商
}
