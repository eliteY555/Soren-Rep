package com.voiceassistant.common.dto;

import com.voiceassistant.common.enums.ChatMode;
import com.voiceassistant.common.enums.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String id;
    private MessageRole role;
    private String content;
    private LocalDateTime timestamp;
    private String providerName;
    private ChatMode mode;
}
