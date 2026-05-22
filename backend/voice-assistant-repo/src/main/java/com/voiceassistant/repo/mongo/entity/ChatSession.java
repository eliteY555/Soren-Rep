package com.voiceassistant.repo.mongo.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "chat_sessions")
public class ChatSession {

    @Id
    private String id;

    private String title = "新对话";

    private List<MessageEntry> messages = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    @Data
    public static class MessageEntry {
        private String role;           // USER / ASSISTANT
        private String content;
        private LocalDateTime timestamp = LocalDateTime.now();
        private String providerName;
        private String mode;           // DIRECT / RAG
    }
}
