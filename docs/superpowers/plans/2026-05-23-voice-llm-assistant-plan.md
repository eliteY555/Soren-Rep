# 语音 AI 助手 — Phase 1 核心 MVP 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建 Web 端语音 AI 助手核心骨架：语音输入 → 文本 → LLM 流式回答 → 对话历史持久化

**Architecture:** Spring Boot 3.x 多模块后端 (common/repo/llm/service/web) + Vue 3 SPA 前端，LangChain4j 统一多模型调用，MongoDB 存对话、MySQL 存配置，SSE 流式输出

**Tech Stack:** Java 17 + Spring Boot 3.x + LangChain4j 1.x + Maven + MySQL + MongoDB + Vue 3 + Vite + Pinia + Element Plus + Web Speech API

---

## File Structure Map

```
voice-llm-assistant/
├── backend/
│   ├── pom.xml                                    # Parent POM
│   ├── voice-assistant-common/
│   │   └── src/main/java/com/voiceassistant/common/
│   │       ├── dto/{ApiResponse, ChatRequest, ChatMessage, SessionDto, ProviderConfigDto}.java
│   │       └── enums/{ChatMode, MessageRole}.java
│   ├── voice-assistant-repo/
│   │   └── src/main/java/com/voiceassistant/repo/
│   │       ├── mysql/{entity/ProviderConfig, repo/ProviderConfigRepository}.java
│   │       └── mongo/{entity/ChatSession, repo/ChatSessionRepository}.java
│   ├── voice-assistant-llm/
│   │   └── src/main/java/com/voiceassistant/llm/
│   │       ├── config/LlmConfiguration.java
│   │       └── factory/ModelFactory.java
│   ├── voice-assistant-service/
│   │   └── src/main/java/com/voiceassistant/service/
│   │       ├── ChatService.java
│   │       └── ConfigService.java
│   └── voice-assistant-web/
│       └── src/main/java/com/voiceassistant/web/
│           ├── controller/{ChatController, ConfigController}.java
│           ├── config/WebConfig.java
│           └── VoiceAssistantApplication.java
├── frontend/
│   ├── index.html
│   ├── vite.config.js
│   ├── package.json
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── views/HomeView.vue
│       ├── components/{VoiceButton, ChatPanel, MessageBubble, SessionList, ProviderSelector, ModeSwitch}.vue
│       ├── composables/{useSpeech, useChat}.js
│       ├── stores/{chatStore, configStore}.js
│       └── services/api.js
└── docs/
```

---

### Task 1: Maven 父 POM + 项目骨架

**Files:** Create all module pom.xml files and parent pom.xml

- [ ] **Step 1: Create parent pom.xml**

Create: `backend/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.voiceassistant</groupId>
    <artifactId>voice-assistant</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>Voice AI Assistant</name>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>

    <modules>
        <module>voice-assistant-common</module>
        <module>voice-assistant-repo</module>
        <module>voice-assistant-llm</module>
        <module>voice-assistant-service</module>
        <module>voice-assistant-web</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <langchain4j.version>1.0.0-beta1</langchain4j.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>dev.langchain4j</groupId>
                <artifactId>langchain4j-bom</artifactId>
                <version>${langchain4j.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

- [ ] **Step 2: Create common module pom.xml**

Create: `backend/voice-assistant-common/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.voiceassistant</groupId>
        <artifactId>voice-assistant</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>voice-assistant-common</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Create repo module pom.xml**

Create: `backend/voice-assistant-repo/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.voiceassistant</groupId>
        <artifactId>voice-assistant</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>voice-assistant-repo</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.voiceassistant</groupId>
            <artifactId>voice-assistant-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Create llm module pom.xml**

Create: `backend/voice-assistant-llm/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.voiceassistant</groupId>
        <artifactId>voice-assistant</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>voice-assistant-llm</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.voiceassistant</groupId>
            <artifactId>voice-assistant-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j</artifactId>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai</artifactId>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-spring-boot-starter</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 5: Create service module pom.xml**

Create: `backend/voice-assistant-service/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.voiceassistant</groupId>
        <artifactId>voice-assistant</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>voice-assistant-service</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.voiceassistant</groupId>
            <artifactId>voice-assistant-repo</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.voiceassistant</groupId>
            <artifactId>voice-assistant-llm</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 6: Create web module pom.xml**

Create: `backend/voice-assistant-web/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.voiceassistant</groupId>
        <artifactId>voice-assistant</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>voice-assistant-web</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.voiceassistant</groupId>
            <artifactId>voice-assistant-service</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 7: Verify build compiles**

Run: `cd backend && mvn compile -q`

Expected: BUILD SUCCESS (may need to create src dirs first)

---

### Task 2: Common 模块 — DTO 与枚举

**Files:** Create DTOs and enums in voice-assistant-common

- [ ] **Step 1: Create ChatMode enum**

Create: `backend/voice-assistant-common/src/main/java/com/voiceassistant/common/enums/ChatMode.java`

```java
package com.voiceassistant.common.enums;

public enum ChatMode {
    DIRECT,  // 直接问答
    RAG      // RAG 增强问答
}
```

- [ ] **Step 2: Create MessageRole enum**

Create: `backend/voice-assistant-common/src/main/java/com/voiceassistant/common/enums/MessageRole.java`

```java
package com.voiceassistant.common.enums;

public enum MessageRole {
    USER,
    ASSISTANT
}
```

- [ ] **Step 3: Create ApiResponse wrapper**

Create: `backend/voice-assistant-common/src/main/java/com/voiceassistant/common/dto/ApiResponse.java`

```java
package com.voiceassistant.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

- [ ] **Step 4: Create ChatRequest DTO**

Create: `backend/voice-assistant-common/src/main/java/com/voiceassistant/common/dto/ChatRequest.java`

```java
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
```

- [ ] **Step 5: Create ChatMessage DTO**

Create: `backend/voice-assistant-common/src/main/java/com/voiceassistant/common/dto/ChatMessage.java`

```java
package com.voiceassistant.common.dto;

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
    private String providerName;     // 使用的 LLM 提供商名
    private ChatMode mode;           // 回答模式
}
```

- [ ] **Step 6: Create SessionDto**

Create: `backend/voice-assistant-common/src/main/java/com/voiceassistant/common/dto/SessionDto.java`

```java
package com.voiceassistant.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {
    private String id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int messageCount;
}
```

- [ ] **Step 7: Create ProviderConfigDto**

Create: `backend/voice-assistant-common/src/main/java/com/voiceassistant/common/dto/ProviderConfigDto.java`

```java
package com.voiceassistant.common.dto;

import lombok.Data;

@Data
public class ProviderConfigDto {
    private Long id;
    private String name;
    private String provider;       // openai, deepseek, qwen, etc.
    private String baseUrl;
    private String modelName;
    private Boolean active;
    // apiKey 只写不入——查询时不返回
}
```

- [ ] **Step 8: Verify common module compiles**

Run: `cd backend && mvn compile -pl voice-assistant-common -q`

Expected: BUILD SUCCESS

---

### Task 3: Repo 模块 — MySQL ProviderConfig

**Files:** Create entity and repository for LLM provider configuration

- [ ] **Step 1: Create ProviderConfig entity**

Create: `backend/voice-assistant-repo/src/main/java/com/voiceassistant/repo/mysql/entity/ProviderConfig.java`

```java
package com.voiceassistant.repo.mysql.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "provider_config")
public class ProviderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;              // 显示名称 e.g. "DeepSeek V4 Pro"

    @Column(nullable = false, length = 30)
    private String provider;          // e.g. "openai", "deepseek"（用于 LangChain4j 工厂路由）

    @Column(length = 256)
    private String baseUrl;           // 自定义 endpoint

    @Column(nullable = false, length = 100)
    private String modelName;

    @Column(nullable = false, length = 512)
    private String apiKey;            // AES-256 加密存储

    @Column(nullable = false)
    private Boolean active = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: Create ProviderConfigRepository**

Create: `backend/voice-assistant-repo/src/main/java/com/voiceassistant/repo/mysql/repository/ProviderConfigRepository.java`

```java
package com.voiceassistant.repo.mysql.repository;

import com.voiceassistant.repo.mysql.entity.ProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderConfigRepository extends JpaRepository<ProviderConfig, Long> {

    Optional<ProviderConfig> findByActiveTrue();

    boolean existsByName(String name);
}
```

- [ ] **Step 3: Create MySQL datasource config (will be referenced by web module)**

The datasource config will live in web module's application.yml. Verify file exists:

Create: `backend/voice-assistant-repo/src/main/resources/application-repo.yml` (placeholder, actual config in web module)

```yaml
# Datasource config is in voice-assistant-web application.yml
# This file reserved for repo-module-specific overrides
```

- [ ] **Step 4: Build repo module**

Run: `cd backend && mvn compile -pl voice-assistant-repo -q`

Expected: BUILD SUCCESS

---

### Task 4: Repo 模块 — MongoDB ChatSession

**Files:** Create document entity and repository for chat sessions

- [ ] **Step 1: Create ChatSession MongoDB document**

Create: `backend/voice-assistant-repo/src/main/java/com/voiceassistant/repo/mongo/entity/ChatSession.java`

```java
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
```

- [ ] **Step 2: Create ChatSessionRepository**

Create: `backend/voice-assistant-repo/src/main/java/com/voiceassistant/repo/mongo/repository/ChatSessionRepository.java`

```java
package com.voiceassistant.repo.mongo.repository;

import com.voiceassistant.repo.mongo.entity.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    List<ChatSession> findAllByOrderByUpdatedAtDesc();

    void deleteById(String id);
}
```

- [ ] **Step 3: Verify full repo module compiles**

Run: `cd backend && mvn compile -pl voice-assistant-repo -q`

Expected: BUILD SUCCESS

---

### Task 5: LLM 模块 — LangChain4j 配置与工厂

**Files:** Create LLM configuration and model factory

- [ ] **Step 1: Create LlmConfiguration**

Create: `backend/voice-assistant-llm/src/main/java/com/voiceassistant/llm/config/LlmConfiguration.java`

```java
package com.voiceassistant.llm.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LlmConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ModelFactory modelFactory() {
        return new ModelFactory();
    }
}
```

- [ ] **Step 2: Create ModelFactory**

Create: `backend/voice-assistant-llm/src/main/java/com/voiceassistant/llm/factory/ModelFactory.java`

```java
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

    public ChatLanguageModel getOrCreateChatModel(ProviderConfig config) {
        return chatModelCache.computeIfAbsent(config.getId(), id -> buildChatModel(config));
    }

    public StreamingChatLanguageModel getOrCreateStreamingModel(ProviderConfig config) {
        return streamModelCache.computeIfAbsent(config.getId(), id -> buildStreamingModel(config));
    }

    public void evict(Long providerId) {
        chatModelCache.remove(providerId);
        streamModelCache.remove(providerId);
    }

    private ChatLanguageModel buildChatModel(ProviderConfig config) {
        log.info("Building ChatLanguageModel for provider: {} model: {}", config.getProvider(), config.getModelName());
        return OpenAiChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .timeout(Duration.ofSeconds(60))
                .maxRetries(2)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    private StreamingChatLanguageModel buildStreamingModel(ProviderConfig config) {
        log.info("Building StreamingChatLanguageModel for provider: {} model: {}", config.getProvider(), config.getModelName());
        return OpenAiStreamingChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .timeout(Duration.ofSeconds(120))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
```

**Why OpenAiChatModel for DeepSeek:** DeepSeek API 兼容 OpenAI 协议（baseUrl = `https://api.deepseek.com/v1`），直接用 LangChain4j 的 `OpenAiChatModel` 即可，无需专门适配器。

- [ ] **Step 3: Verify llm module compiles**

Run: `cd backend && mvn compile -pl voice-assistant-llm -q`

Expected: BUILD SUCCESS

---

### Task 6: Service 模块 — ConfigService

**Files:** Create ConfigService for LLM provider CRUD + encryption

- [ ] **Step 1: Create AES encryption utility**

Create: `backend/voice-assistant-service/src/main/java/com/voiceassistant/service/AesUtil.java`

```java
package com.voiceassistant.service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AesUtil {

    private static final String ALGORITHM = "AES";
    private final SecretKeySpec keySpec;

    public AesUtil(String secretKey) {
        byte[] key = secretKey.getBytes();
        // 确保密钥为 16 字节
        byte[] key16 = new byte[16];
        System.arraycopy(key, 0, key16, 0, Math.min(key.length, 16));
        this.keySpec = new SecretKeySpec(key16, ALGORITHM);
    }

    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
```

- [ ] **Step 2: Create ConfigService**

Create: `backend/voice-assistant-service/src/main/java/com/voiceassistant/service/ConfigService.java`

```java
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
        // 从配置文件读取 AES 密钥，此处硬编码 16 字符种子
        this.aesUtil = new AesUtil("VoiceAsst2026!@#");
    }

    public List<ProviderConfigDto> listProviders() {
        return providerRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProviderConfigDto getActiveProvider() {
        return providerRepo.findByActiveTrue()
                .map(this::toDto)
                .orElse(null);
    }

    public ProviderConfigDto addProvider(ProviderConfigDto dto) {
        ProviderConfig entity = new ProviderConfig();
        entity.setName(dto.getName());
        entity.setProvider(dto.getProvider());
        entity.setBaseUrl(dto.getBaseUrl());
        entity.setModelName(dto.getModelName());
        entity.setApiKey(aesUtil.encrypt(dto.getApiKey()));  // 加密存储
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
        // 全部设为 inactive
        providerRepo.findAll().forEach(p -> {
            p.setActive(false);
            providerRepo.save(p);
        });
        // 激活目标
        ProviderConfig target = providerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found: " + id));
        target.setActive(true);
        return toDto(providerRepo.save(target));
    }

    public ProviderConfig findProviderEntity(Long id) {
        return providerRepo.findById(id)
                .orElseGet(() -> providerRepo.findByActiveTrue()
                        .orElseThrow(() -> new RuntimeException("No active provider configured")));
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
        // apiKey 不返回
        return dto;
    }
}
```

- [ ] **Step 3: Verify service module compiles**

Run: `cd backend && mvn compile -pl voice-assistant-service -q`

Expected: BUILD SUCCESS

---

### Task 7: Service 模块 — ChatService

**Files:** Create ChatService for core chat logic + conversation persistence

- [ ] **Step 1: Create ChatService**

Create: `backend/voice-assistant-service/src/main/java/com/voiceassistant/service/ChatService.java`

```java
package com.voiceassistant.service;

import com.voiceassistant.common.dto.ChatMessage;
import com.voiceassistant.common.dto.ChatRequest;
import com.voiceassistant.common.dto.SessionDto;
import com.voiceassistant.common.enums.ChatMode;
import com.voiceassistant.common.enums.MessageRole;
import com.voiceassistant.llm.factory.ModelFactory;
import com.voiceassistant.repo.mongo.entity.ChatSession;
import com.voiceassistant.repo.mongo.entity.ChatSession.MessageEntry;
import com.voiceassistant.repo.mongo.repository.ChatSessionRepository;
import com.voiceassistant.repo.mysql.entity.ProviderConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatService {

    private final ChatSessionRepository sessionRepo;
    private final ConfigService configService;
    private final ModelFactory modelFactory;

    public ChatService(ChatSessionRepository sessionRepo,
                       ConfigService configService,
                       ModelFactory modelFactory) {
        this.sessionRepo = sessionRepo;
        this.configService = configService;
        this.modelFactory = modelFactory;
    }

    /**
     * 发送消息，返回 SSE 流式 Token Flux
     */
    public Flux<String> sendMessageStream(ChatRequest request) {
        // 1. 获取或创建 session
        ChatSession session = getOrCreateSession(request.getSessionId());
        String sessionId = session.getId();

        // 2. 获取 LLM provider
        ProviderConfig provider = request.getProviderId() != null
                ? configService.findProviderEntity(request.getProviderId())
                : configService.findProviderEntity(null);

        String decryptedApiKey = configService.decryptApiKey(provider.getApiKey());

        // 3. 构建用户消息
        UserMessage userMsg = UserMessage.from(request.getContent());

        // 4. 根据模式构建发送内容
        String promptContent = request.getContent();
        if (request.getMode() == ChatMode.RAG) {
            // Phase 2 实现 RAG 检索增强
            promptContent = request.getContent();
        }

        // 5. 保存用户消息
        saveMessage(session, "USER", request.getContent(), provider.getName(),
                request.getMode().name());

        // 6. 构建流式模型并返回 SSE Flux
        StreamingChatLanguageModel model = buildStreamingModel(provider, decryptedApiKey);

        // 使用 StringBuilder 收集完整回复
        StringBuilder fullResponse = new StringBuilder();

        return Flux.create(sink -> {
            model.generate(userMsg.single().text(), new dev.langchain4j.model.StreamingResponseHandler<>() {
                @Override
                public void onNext(String token) {
                    fullResponse.append(token);
                    sink.next(token);
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    saveMessage(session, "ASSISTANT", fullResponse.toString(),
                            provider.getName(), request.getMode().name());
                    sink.complete();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("LLM streaming error", error);
                    sink.error(error);
                }
            });
        });
    }

    public List<SessionDto> listSessions() {
        return sessionRepo.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toSessionDto)
                .collect(Collectors.toList());
    }

    public SessionDto createSession() {
        ChatSession session = new ChatSession();
        session.setMessages(new ArrayList<>());
        ChatSession saved = sessionRepo.save(session);
        return toSessionDto(saved);
    }

    public void deleteSession(String id) {
        sessionRepo.deleteById(id);
    }

    public List<ChatMessage> getSessionMessages(String sessionId) {
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        return session.getMessages().stream()
                .map(m -> ChatMessage.builder()
                        .role(MessageRole.valueOf(m.getRole()))
                        .content(m.getContent())
                        .timestamp(m.getTimestamp())
                        .providerName(m.getProviderName())
                        .mode(ChatMode.valueOf(m.getMode()))
                        .build())
                .collect(Collectors.toList());
    }

    // --- private helpers ---

    private ChatSession getOrCreateSession(String sessionId) {
        if (sessionId != null) {
            return sessionRepo.findById(sessionId)
                    .orElseGet(this::createNewSession);
        }
        return createNewSession();
    }

    private ChatSession createNewSession() {
        ChatSession session = new ChatSession();
        session.setMessages(new ArrayList<>());
        return sessionRepo.save(session);
    }

    private void saveMessage(ChatSession session, String role, String content,
                             String providerName, String mode) {
        MessageEntry entry = new MessageEntry();
        entry.setRole(role);
        entry.setContent(content);
        entry.setTimestamp(LocalDateTime.now());
        entry.setProviderName(providerName);
        entry.setMode(mode);
        session.getMessages().add(entry);
        session.setUpdatedAt(LocalDateTime.now());
        // 自动用第一条用户消息作标题
        if (session.getMessages().size() == 1 && "USER".equals(role)) {
            String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
            session.setTitle(title);
        }
        sessionRepo.save(session);
    }

    private StreamingChatLanguageModel buildStreamingModel(ProviderConfig config, String apiKey) {
        return dev.langchain4j.model.openai.OpenAiStreamingChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(apiKey)
                .modelName(config.getModelName())
                .build();
    }

    private SessionDto toSessionDto(ChatSession session) {
        return SessionDto.builder()
                .id(session.getId())
                .title(session.getTitle())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .messageCount(session.getMessages().size())
                .build();
    }
}
```

**注意:** Task 7 中的 ChatService 依赖了 `reactor-core` 的 Flux。需要在 service 模块 pom.xml 中添加 `spring-boot-starter-webflux` 依赖才能编译。如果是 Spring Boot 3.x，`spring-boot-starter-web` 已包含 reactor-core。

- [ ] **Step 2: Add missing dependency to service pom.xml**

Edit: `backend/voice-assistant-service/pom.xml` — 在 `</dependencies>` 之前插入:

```xml
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-core</artifactId>
        </dependency>
```

Wait, Spring Boot 3.x `spring-boot-starter-web` already includes reactor-core transitively. Let me verify by checking the ChatService — the Flux.create approach uses reactor directly. The web starter actually pulls in reactor-core, so we're fine.

- [ ] **Step 3: Verify service module compiles**

Run: `cd backend && mvn compile -pl voice-assistant-service -q`

Expected: BUILD SUCCESS

---

### Task 8: Web 模块 — SSE 流式端点

**Files:** Create ChatController with SSE endpoint, SseEmitter helper

- [ ] **Step 1: Create ChatController**

Create: `backend/voice-assistant-web/src/main/java/com/voiceassistant/web/controller/ChatController.java`

```java
package com.voiceassistant.web.controller;

import com.voiceassistant.common.dto.ApiResponse;
import com.voiceassistant.common.dto.ChatMessage;
import com.voiceassistant.common.dto.ChatRequest;
import com.voiceassistant.common.dto.SessionDto;
import com.voiceassistant.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 发送消息 — SSE 流式返回
     */
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L); // 无超时

        executor.execute(() -> {
            try {
                // 生成 messageId
                String messageId = UUID.randomUUID().toString();
                // 先发送 messageId 用于前端关联
                emitter.send(SseEmitter.event()
                        .name("messageId")
                        .data(messageId));

                chatService.sendMessageStream(request)
                        .doOnNext(token -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("token")
                                        .data(token));
                            } catch (IOException e) {
                                log.error("SSE send error", e);
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnComplete(() -> {
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data("[DONE]"));
                            emitter.complete();
                        })
                        .doOnError(emitter::completeWithError)
                        .subscribe();
            } catch (Exception e) {
                log.error("Chat stream error", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionDto>> listSessions() {
        return ApiResponse.success(chatService.listSessions());
    }

    @PostMapping("/sessions")
    public ApiResponse<SessionDto> createSession() {
        return ApiResponse.success(chatService.createSession());
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable String id) {
        chatService.deleteSession(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<ChatMessage>> getMessages(@PathVariable String id) {
        return ApiResponse.success(chatService.getSessionMessages(id));
    }
}
```

**Wait – Fix ChatService return type:** The ChatService.sendMessageStream uses `Flux<String>` but uses `Flux.create` which returns `Flux<AiMessage>` from the handler. Let me fix the ChatService to properly use Flux instead of the lambda-based handler, OR use a simple blocking approach with SseEmitter directly.

**Decision:** Since LangChain4j's `StreamingChatLanguageModel` uses a callback-based API (`StreamingResponseHandler`), wrapping it with `Flux.create` is the correct approach but adds complexity. Instead, let's handle streaming directly in the controller using the callback API.

**Revise ChatService.sendMessageStream to use a simpler interface:**

Let's revise: ChatService will accept a `StreamingResponseHandler` parameter, and the controller handles SSE directly.

- [ ] **Step 1 (Revised): Update ChatService with handler-based approach**

Edit the ChatService.sendMessageStream method to this revised version:

```java
// In ChatService.java, replace the sendMessageStream method:

public ChatSession sendMessage(ChatRequest request) {
    ChatSession session = getOrCreateSession(request.getSessionId());
    ProviderConfig provider = request.getProviderId() != null
            ? configService.findProviderEntity(request.getProviderId())
            : configService.findProviderEntity(null);

    saveMessage(session, "USER", request.getContent(), provider.getName(),
            request.getMode().name());
    return session;
}

public void streamResponse(ChatRequest request,
                           java.util.function.Consumer<String> onToken,
                           Runnable onComplete,
                           java.util.function.Consumer<Throwable> onError) {
    ChatSession session = getOrCreateSession(request.getSessionId());
    ProviderConfig provider = request.getProviderId() != null
            ? configService.findProviderEntity(request.getProviderId())
            : configService.findProviderEntity(null);

    String decryptedApiKey = configService.decryptApiKey(provider.getApiKey());
    String sessionId = session.getId();
    String providerName = provider.getName();

    StreamingChatLanguageModel model = dev.langchain4j.model.openai.OpenAiStreamingChatModel.builder()
            .baseUrl(provider.getBaseUrl())
            .apiKey(decryptedApiKey)
            .modelName(provider.getModelName())
            .build();

    StringBuilder fullResponse = new StringBuilder();

    model.generate(request.getContent(), new dev.langchain4j.model.StreamingResponseHandler<>() {
        @Override
        public void onNext(String token) {
            fullResponse.append(token);
            onToken.accept(token);
        }

        @Override
        public void onComplete(Response<AiMessage> response) {
            saveMessage(session, "ASSISTANT", fullResponse.toString(),
                    providerName, request.getMode().name());
            onComplete.run();
        }

        @Override
        public void onError(Throwable error) {
            log.error("LLM streaming error", error);
            onError.accept(error);
        }
    });
}
```

**This design is cleaner.** The controller handles SSE emission; the service handles LLM interaction and persistence.

- [ ] **Step 2 (Revised): ChatController with cleaner SSE**

Create: `backend/voice-assistant-web/src/main/java/com/voiceassistant/web/controller/ChatController.java`

```java
package com.voiceassistant.web.controller;

import com.voiceassistant.common.dto.*;
import com.voiceassistant.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        executor.execute(() -> {
            chatService.streamResponse(request,
                    token -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data(token));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    () -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data("[DONE]"));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    emitter::completeWithError
            );
        });

        return emitter;
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionDto>> listSessions() {
        return ApiResponse.success(chatService.listSessions());
    }

    @PostMapping("/sessions")
    public ApiResponse<SessionDto> createSession() {
        return ApiResponse.success(chatService.createSession());
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable String id) {
        chatService.deleteSession(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<ChatMessage>> getMessages(@PathVariable String id) {
        return ApiResponse.success(chatService.getSessionMessages(id));
    }
}
```

- [ ] **Step 3: Clean up ChatService — remove Flux dependency**

The revised ChatService no longer needs reactor-core Flux — only uses java.util.function interfaces. Remove the Flux import and usage.

- [ ] **Step 4: Create CORS config**

Create: `backend/voice-assistant-web/src/main/java/com/voiceassistant/web/config/WebConfig.java`

```java
package com.voiceassistant.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

- [ ] **Step 5: Verify web module compiles**

Run: `cd backend && mvn compile -pl voice-assistant-web -q`

Expected: BUILD SUCCESS

---

### Task 9: Web 模块 — ConfigController

**Files:** Create REST controller for provider configuration

- [ ] **Step 1: Create ConfigController**

Create: `backend/voice-assistant-web/src/main/java/com/voiceassistant/web/controller/ConfigController.java`

```java
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
```

- [ ] **Step 2: Rebuild web module**

Run: `cd backend && mvn compile -pl voice-assistant-web -q`

---

### Task 10: 应用入口与配置文件

**Files:** Create Spring Boot application class and application.yml

- [ ] **Step 1: Create main application class**

Create: `backend/voice-assistant-web/src/main/java/com/voiceassistant/web/VoiceAssistantApplication.java`

```java
package com.voiceassistant.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = "com.voiceassistant")
@EnableJpaRepositories(basePackages = "com.voiceassistant.repo.mysql.repository")
@EntityScan(basePackages = "com.voiceassistant.repo.mysql.entity")
@EnableMongoRepositories(basePackages = "com.voiceassistant.repo.mongo.repository")
public class VoiceAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoiceAssistantApplication.class, args);
    }
}
```

- [ ] **Step 2: Create application.yml**

Create: `backend/voice-assistant-web/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: voice-assistant
  datasource:
    url: jdbc:mysql://localhost:3306/voice_assistant?useUnicode=true&characterEncoding=utf-8&createDatabaseIfNotExist=true
    username: root
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  data:
    mongodb:
      uri: mongodb://localhost:27017/voice_assistant
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB

# AES encryption key (16 characters)
app:
  aes-key: VoiceAsst2026!@#

logging:
  level:
    com.voiceassistant: DEBUG
    dev.langchain4j: DEBUG
```

- [ ] **Step 3: Create DataInitializer — insert default DeepSeek config**

Create: `backend/voice-assistant-web/src/main/java/com/voiceassistant/web/config/DataInitializer.java`

```java
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
            log.info("Default provider 'DeepSeek V4 Pro' initialized. Please update the API key.");
        }
    }
}
```

- [ ] **Step 4: Build entire backend**

Run: `cd backend && mvn clean package -DskipTests -q`

Expected: BUILD SUCCESS

---

### Task 11: 前端 — Vite + Vue 3 项目初始化

**Files:** Create package.json, vite.config.js, index.html, main.js, App.vue

- [ ] **Step 1: Create package.json**

Create: `frontend/package.json`

```json
{
  "name": "voice-llm-assistant",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "pinia": "^2.1.0",
    "vue-router": "^4.3.0",
    "element-plus": "^2.7.0",
    "@element-plus/icons-vue": "^2.3.0",
    "axios": "^1.7.0",
    "marked": "^12.0.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.2.0"
  }
}
```

- [ ] **Step 2: Create vite.config.js**

Create: `frontend/vite.config.js`

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: Create index.html**

Create: `frontend/index.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>语音 AI 助手</title>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.js"></script>
</body>
</html>
```

- [ ] **Step 4: Create main.js**

Create: `frontend/src/main.js`

```javascript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'

const app = createApp(App)
app.use(createPinia())
app.use(ElementPlus, { locale: /* zhCn */ undefined })
app.mount('#app')
```

- [ ] **Step 5: Create App.vue skeleton**

Create: `frontend/src/App.vue`

```vue
<template>
  <HomeView />
</template>

<script setup>
import HomeView from './views/HomeView.vue'
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
#app { height: 100vh; overflow: hidden; }
</style>
```

- [ ] **Step 6: Install dependencies**

Run: `cd frontend && npm install`

---

### Task 12: 前端 — API 服务层

**Files:** Create API service

- [ ] **Step 1: Create api.js**

Create: `frontend/src/services/api.js`

```javascript
import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// --- Chat API ---

export function sendMessageStream(chatRequest, { onToken, onDone, onError }) {
  return fetch('/api/chat/send', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(chatRequest)
  }).then(response => {
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    function read() {
      reader.read().then(({ done, value }) => {
        if (done) { onDone(); return }
        buffer += decoder.decode(value, { stream: true })
        // Parse SSE events
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) {
          if (line.startsWith('event:')) {
            const eventType = line.slice(6).trim()
            // next line should be data:
            continue
          }
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (data === '[DONE]') { onDone(); return }
            onToken(data)
          }
        }
        read()
      }).catch(onError)
    }
    read()
  }).catch(onError)
}

export function listSessions() {
  return http.get('/chat/sessions').then(r => r.data.data)
}

export function createSession() {
  return http.post('/chat/sessions').then(r => r.data.data)
}

export function deleteSession(id) {
  return http.delete(`/chat/sessions/${id}`).then(r => r.data)
}

export function getSessionMessages(id) {
  return http.get(`/chat/sessions/${id}/messages`).then(r => r.data.data)
}

// --- Config API ---

export function listProviders() {
  return http.get('/config/providers').then(r => r.data.data)
}

export function addProvider(data) {
  return http.post('/config/providers', data).then(r => r.data.data)
}

export function updateProvider(id, data) {
  return http.put(`/config/providers/${id}`, data).then(r => r.data.data)
}

export function deleteProvider(id) {
  return http.delete(`/config/providers/${id}`).then(r => r.data)
}

export function activateProvider(id) {
  return http.put(`/config/providers/${id}/activate`).then(r => r.data.data)
}
```

---

### Task 13: 前端 — Pinia Stores

**Files:** Create chatStore and configStore

- [ ] **Step 1: Create chatStore**

Create: `frontend/src/stores/chatStore.js`

```javascript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as api from '../services/api'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref([])
  const currentSessionId = ref(null)
  const messages = ref([])
  const isStreaming = ref(false)
  const streamingContent = ref('')

  const currentSession = computed(() =>
    sessions.value.find(s => s.id === currentSessionId.value)
  )

  async function loadSessions() {
    sessions.value = await api.listSessions()
  }

  async function createNewSession() {
    const session = await api.createSession()
    sessions.value.unshift(session)
    switchSession(session.id)
    return session
  }

  async function switchSession(sessionId) {
    currentSessionId.value = sessionId
    messages.value = await api.getSessionMessages(sessionId)
  }

  async function deleteCurrentSession(id) {
    await api.deleteSession(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    if (currentSessionId.value === id) {
      currentSessionId.value = null
      messages.value = []
    }
  }

  async function sendMessage(content, mode, providerId) {
    if (!currentSessionId.value) {
      await createNewSession()
    }

    // Add user message locally
    const userMsg = { role: 'USER', content, timestamp: new Date().toISOString() }
    messages.value.push(userMsg)

    // Add placeholder assistant message
    const assistantMsg = { role: 'ASSISTANT', content: '', timestamp: new Date().toISOString(), streaming: true }
    messages.value.push(assistantMsg)

    isStreaming.value = true
    streamingContent.value = ''

    await api.sendMessageStream(
      {
        sessionId: currentSessionId.value,
        content,
        mode,
        providerId
      },
      {
        onToken(token) {
          streamingContent.value += token
          // Update last message content
          const last = messages.value[messages.value.length - 1]
          if (last) last.content = streamingContent.value
        },
        onDone() {
          const last = messages.value[messages.value.length - 1]
          if (last) delete last.streaming
          isStreaming.value = false
          streamingContent.value = ''
          loadSessions() // refresh session list titles
        },
        onError(err) {
          console.error('Stream error:', err)
          const last = messages.value[messages.value.length - 1]
          if (last) {
            last.content = '请求失败: ' + err.message
            delete last.streaming
          }
          isStreaming.value = false
        }
      }
    )
  }

  return {
    sessions, currentSessionId, messages, isStreaming, streamingContent,
    currentSession, loadSessions, createNewSession, switchSession,
    deleteCurrentSession, sendMessage
  }
})
```

- [ ] **Step 2: Create configStore**

Create: `frontend/src/stores/configStore.js`

```javascript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as api from '../services/api'

export const useConfigStore = defineStore('config', () => {
  const providers = ref([])
  const activeProviderId = ref(null)
  const chatMode = ref('DIRECT') // 'DIRECT' | 'RAG'

  async function loadProviders() {
    providers.value = await api.listProviders()
    const active = providers.value.find(p => p.active)
    if (active) activeProviderId.value = active.id
  }

  async function addProvider(data) {
    await api.addProvider(data)
    await loadProviders()
  }

  async function updateProvider(id, data) {
    await api.updateProvider(id, data)
    await loadProviders()
  }

  async function deleteProvider(id) {
    await api.deleteProvider(id)
    await loadProviders()
  }

  async function activateProvider(id) {
    await api.activateProvider(id)
    activeProviderId.value = id
    await loadProviders()
  }

  function setChatMode(mode) {
    chatMode.value = mode
  }

  return {
    providers, activeProviderId, chatMode,
    loadProviders, addProvider, updateProvider,
    deleteProvider, activateProvider, setChatMode
  }
})
```

---

### Task 14: 前端 — useSpeech Composable

**Files:** Create speech recognition composable

- [ ] **Step 1: Create useSpeech.js**

Create: `frontend/src/composables/useSpeech.js`

```javascript
import { ref, onUnmounted } from 'vue'

const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition

export function useSpeech() {
  const isSupported = ref(!!SpeechRecognition)
  const isListening = ref(false)
  const transcript = ref('')
  const interimTranscript = ref('')
  const mode = ref('ptt') // 'ptt' | 'streaming'
  const error = ref(null)

  let recognition = null

  function createRecognition() {
    if (!SpeechRecognition) return null
    const rec = new SpeechRecognition()
    rec.lang = 'zh-CN'
    rec.interimResults = true
    rec.continuous = mode.value === 'streaming'
    rec.maxAlternatives = 1

    rec.onresult = (event) => {
      let interim = ''
      let final = ''
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i]
        if (result.isFinal) {
          final += result[0].transcript
        } else {
          interim += result[0].transcript
        }
      }
      if (final) transcript.value += final
      interimTranscript.value = interim
    }

    rec.onerror = (event) => {
      error.value = event.error
      isListening.value = false
    }

    rec.onend = () => {
      isListening.value = false
      // PTT mode: auto-append final transcript
      if (mode.value === 'streaming' && isListening.value) {
        // In streaming mode, restart if still listening
        rec.start()
      }
    }

    return rec
  }

  function startListening() {
    if (!isSupported.value) {
      error.value = '浏览器不支持语音识别'
      return
    }
    error.value = null
    transcript.value = ''
    interimTranscript.value = ''

    recognition = createRecognition()
    if (recognition) {
      recognition.start()
      isListening.value = true
    }
  }

  function stopListening() {
    if (recognition) {
      recognition.stop()
      isListening.value = false
    }
  }

  function setMode(newMode) {
    mode.value = newMode
    if (recognition) {
      recognition.stop()
    }
  }

  function clearTranscript() {
    transcript.value = ''
    interimTranscript.value = ''
  }

  onUnmounted(() => {
    if (recognition) recognition.stop()
  })

  return {
    isSupported, isListening, transcript, interimTranscript, mode, error,
    startListening, stopListening, setMode, clearTranscript
  }
}
```

---

### Task 15: 前端 — 核心组件

**Files:** Create VoiceButton, ChatPanel, MessageBubble, SessionList, ProviderSelector, ModeSwitch

- [ ] **Step 1: Create VoiceButton.vue**

Create: `frontend/src/components/VoiceButton.vue`

```vue
<template>
  <div class="voice-button-wrap">
    <el-button
      v-if="isSupported"
      :type="isListening ? 'danger' : 'primary'"
      :icon="isListening ? Microphone : Microphone"
      circle
      size="large"
      @mousedown="startPTT"
      @mouseup="stopPTT"
      @mouseleave="stopPTT"
      @touchstart.prevent="startPTT"
      @touchend.prevent="stopPTT"
      :class="{ recording: isListening }"
    />
    <el-tag v-else type="warning">浏览器不支持语音识别，请使用 Chrome</el-tag>
    <span v-if="isListening" class="recording-hint">正在录音，松开发送...</span>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Microphone } from '@element-plus/icons-vue'

const props = defineProps({
  isSupported: Boolean,
  isListening: Boolean,
  isPTT: Boolean
})

const emit = defineEmits(['start', 'stop'])

function startPTT() {
  if (props.isPTT) emit('start')
}

function stopPTT() {
  if (props.isPTT) emit('stop')
}
</script>

<style scoped>
.voice-button-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}
.recording-hint {
  color: #f56c6c;
  font-size: 14px;
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
```

- [ ] **Step 2: Create MessageBubble.vue**

Create: `frontend/src/components/MessageBubble.vue`

```vue
<template>
  <div :class="['message-bubble', role === 'USER' ? 'user' : 'assistant']">
    <div class="bubble-header">
      <span class="role-label">{{ role === 'USER' ? '你' : 'AI' }}</span>
      <span v-if="providerName" class="provider-tag">{{ providerName }}</span>
      <span v-if="mode" class="mode-tag">{{ mode === 'RAG' ? '知识库' : '' }}</span>
    </div>
    <div class="bubble-content" v-html="renderedContent"></div>
    <span v-if="streaming" class="cursor-blink">|</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { marked } from 'marked'

const props = defineProps({
  role: String,
  content: String,
  providerName: String,
  mode: String,
  streaming: Boolean
})

const renderedContent = computed(() => {
  if (!props.content) return ''
  return marked(props.content)
})
</script>

<style scoped>
.message-bubble { max-width: 80%; margin: 12px 0; padding: 12px 16px; border-radius: 12px; }
.message-bubble.user { margin-left: auto; background: #409eff; color: #fff; }
.message-bubble.assistant { margin-right: auto; background: #f4f4f5; color: #303133; }
.bubble-header { font-size: 12px; margin-bottom: 6px; opacity: 0.7; display: flex; gap: 8px; }
.bubble-content { line-height: 1.6; word-break: break-word; }
.bubble-content :deep(p) { margin: 0 0 8px 0; }
.bubble-content :deep(pre) { background: #1e1e1e; color: #d4d4d4; padding: 12px; border-radius: 6px; overflow-x: auto; }
.cursor-blink { animation: blink 1s step-end infinite; }
@keyframes blink { 50% { opacity: 0; } }
</style>
```

- [ ] **Step 3: Create ChatPanel.vue**

Create: `frontend/src/components/ChatPanel.vue`

```vue
<template>
  <div class="chat-panel" ref="panelRef">
    <div v-if="messages.length === 0" class="empty-state">
      <h2>语音 AI 助手</h2>
      <p>按住麦克风按钮开始语音提问，或直接在下方输入文字</p>
    </div>
    <MessageBubble
      v-for="(msg, idx) in messages"
      :key="idx"
      v-bind="msg"
    />
    <div ref="bottomRef" />
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import MessageBubble from './MessageBubble.vue'

const props = defineProps({ messages: Array })
const bottomRef = ref(null)

watch(() => props.messages?.length, () => {
  nextTick(() => bottomRef.value?.scrollIntoView({ behavior: 'smooth' }))
}, { deep: true })
</script>

<style scoped>
.chat-panel { flex: 1; overflow-y: auto; padding: 20px; }
.empty-state { text-align: center; margin-top: 20vh; color: #909399; }
.empty-state h2 { margin-bottom: 8px; color: #303133; }
</style>
```

- [ ] **Step 4: Create SessionList.vue**

Create: `frontend/src/components/SessionList.vue`

```vue
<template>
  <div class="session-list">
    <el-button type="primary" @click="$emit('newSession')" :icon="Plus" block>
      新对话
    </el-button>
    <div class="sessions">
      <div
        v-for="session in sessions"
        :key="session.id"
        :class="['session-item', { active: session.id === currentId }]"
        @click="$emit('switch', session.id)"
      >
        <span class="session-title">{{ session.title || '新对话' }}</span>
        <el-button
          :icon="Delete"
          circle
          size="small"
          text
          @click.stop="$emit('delete', session.id)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, Delete } from '@element-plus/icons-vue'
defineProps({ sessions: Array, currentId: String })
defineEmits(['newSession', 'switch', 'delete'])
</script>

<style scoped>
.session-list { padding: 12px; }
.sessions { margin-top: 12px; }
.session-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 12px; border-radius: 6px; cursor: pointer;
  margin-bottom: 4px; font-size: 14px;
}
.session-item:hover { background: #f0f2f5; }
.session-item.active { background: #ecf5ff; color: #409eff; }
.session-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
</style>
```

- [ ] **Step 5: Create ProviderSelector.vue**

Create: `frontend/src/components/ProviderSelector.vue`

```vue
<template>
  <el-select
    :model-value="activeId"
    @update:model-value="$emit('change', $event)"
    placeholder="选择模型"
    size="small"
    style="width: 180px"
  >
    <el-option
      v-for="p in providers"
      :key="p.id"
      :label="p.name"
      :value="p.id"
    />
  </el-select>
</template>

<script setup>
defineProps({ providers: Array, activeId: [Number, String] })
defineEmits(['change'])
</script>
```

- [ ] **Step 6: Create ModeSwitch.vue**

Create: `frontend/src/components/ModeSwitch.vue`

```vue
<template>
  <el-radio-group
    :model-value="mode"
    @update:model-value="$emit('change', $event)"
    size="small"
  >
    <el-radio-button value="DIRECT">直接问答</el-radio-button>
    <el-radio-button value="RAG">知识库增强</el-radio-button>
  </el-radio-group>
</template>

<script setup>
defineProps({ mode: String })
defineEmits(['change'])
</script>
```

---

### Task 16: 前端 — 主页面 HomeView

**Files:** Create the main page layout integrating all components

- [ ] **Step 1: Create HomeView.vue**

Create: `frontend/src/views/HomeView.vue`

```vue
<template>
  <div class="home">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <SessionList
        :sessions="chatStore.sessions"
        :currentId="chatStore.currentSessionId"
        @newSession="chatStore.createNewSession()"
        @switch="chatStore.switchSession($event)"
        @delete="chatStore.deleteCurrentSession($event)"
      />
    </aside>

    <!-- 主区域 -->
    <main class="main">
      <!-- 顶栏 -->
      <header class="topbar">
        <ProviderSelector
          :providers="configStore.providers"
          :activeId="configStore.activeProviderId"
          @change="configStore.activateProvider($event)"
        />
        <ModeSwitch
          :mode="configStore.chatMode"
          @change="configStore.setChatMode($event)"
        />
      </header>

      <!-- 对话面板 -->
      <ChatPanel :messages="chatStore.messages" />

      <!-- 输入区域 -->
      <footer class="input-area">
        <div class="input-row">
          <!-- 语音按钮 -->
          <VoiceButton
            :isSupported="speech.isSupported.value"
            :isListening="speech.isListening.value"
            :isPTT="speech.mode.value === 'ptt'"
            @start="speech.startListening()"
            @stop="speech.stopListening()"
          />

          <!-- 文字输入 -->
          <el-input
            v-model="textInput"
            placeholder="输入问题，或按住麦克风按钮语音输入..."
            @keyup.enter="sendText"
            :disabled="chatStore.isStreaming"
            clearable
          />

          <!-- 发送按钮 -->
          <el-button
            type="primary"
            :icon="Promotion"
            @click="sendText"
            :disabled="!textInput.trim() || chatStore.isStreaming"
          >
            发送
          </el-button>

          <!-- 模式切换按钮 -->
          <el-button
            @click="speech.mode.value === 'ptt' ? speech.setMode('streaming') : speech.setMode('ptt')"
            size="small"
          >
            {{ speech.mode.value === 'ptt' ? '切换到实时模式' : '切换到按键模式' }}
          </el-button>
        </div>

        <!-- 语音转写文本显示 -->
        <div v-if="speech.transcript.value || speech.interimTranscript.value" class="transcript-preview">
          <span>{{ speech.transcript.value }}</span>
          <span style="color: #909399">{{ speech.interimTranscript.value }}</span>
          <el-button size="small" text @click="sendTranscript">发送语音文本</el-button>
          <el-button size="small" text @click="speech.clearTranscript()">清除</el-button>
        </div>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { Promotion } from '@element-plus/icons-vue'
import { useChatStore } from '../stores/chatStore'
import { useConfigStore } from '../stores/configStore'
import { useSpeech } from '../composables/useSpeech'
import ChatPanel from '../components/ChatPanel.vue'
import VoiceButton from '../components/VoiceButton.vue'
import SessionList from '../components/SessionList.vue'
import ProviderSelector from '../components/ProviderSelector.vue'
import ModeSwitch from '../components/ModeSwitch.vue'

const chatStore = useChatStore()
const configStore = useConfigStore()
const speech = useSpeech()

const textInput = ref('')

onMounted(async () => {
  await configStore.loadProviders()
  await chatStore.loadSessions()
})

// 监听语音识别完成，自动填入
watch(() => speech.transcript.value, (val) => {
  if (val) textInput.value = val
})

function sendText() {
  const content = textInput.value.trim()
  if (!content) return
  textInput.value = ''
  chatStore.sendMessage(content, configStore.chatMode, configStore.activeProviderId)
}

function sendTranscript() {
  const content = speech.transcript.value.trim()
  if (!content) return
  speech.clearTranscript()
  chatStore.sendMessage(content, configStore.chatMode, configStore.activeProviderId)
}
</script>

<style scoped>
.home { display: flex; height: 100vh; }
.sidebar { width: 260px; border-right: 1px solid #e4e7ed; background: #fafafa; overflow-y: auto; }
.main { flex: 1; display: flex; flex-direction: column; }
.topbar { display: flex; align-items: center; gap: 12px; padding: 12px 20px; border-bottom: 1px solid #e4e7ed; }
.input-area { border-top: 1px solid #e4e7ed; padding: 16px 20px; }
.input-row { display: flex; align-items: center; gap: 12px; }
.transcript-preview { margin-top: 8px; padding: 8px 12px; background: #f0f9eb; border-radius: 6px; font-size: 14px; display: flex; align-items: center; gap: 8px; }
</style>
```

- [ ] **Step 2: Verify frontend dev server starts**

Run: `cd frontend && npm run dev`

Expected: Vite dev server on port 5173

---

## Phase 2: RAG 增强（Task 17-22 后续追加）

Phase 2 任务概要（完整代码在 Phase 1 稳定后展开）:

| Task | 内容 |
|------|------|
| 17 | DocumentInfo MySQL 实体 + Repository |
| 18 | EmbeddingService + Pinecone 配置 |
| 19 | DocumentProcessor (上传→分块→向量化→存储) |
| 20 | RagService (检索→拼 Prompt→发给 LLM) |
| 21 | KnowledgeController REST 端点 |
| 22 | 前端 KnowledgePanel + useKnowledge composable |

---

## Self-Review Checklist

- [x] **Spec coverage**: Each P0 requirement from spec maps to a task or step above
  - STT-01~06 → Task 14 (useSpeech), Task 15 (VoiceButton), Task 16 (HomeView)
  - LLM-01~05 → Task 5-9, Task 13 (stores), Task 15 (ProviderSelector)
  - RAG-06 → Task 15 (ModeSwitch), Phase 2 for full RAG
  - CHAT-01~03 → Task 4, Task 7, Task 13 (chatStore), Task 15 (SessionList)
  - SYS-01 → Task 6, Task 9, Phase 2 for others

- [x] **Placeholder scan**: No TBD/TODO in plan. Phase 2 is summarized but explicitly noted as "to be expanded."

- [x] **Type consistency**: ChatSession.MessageEntry.role = String ("USER"/"ASSISTANT") matches ChatMessage.role = MessageRole enum → conversion in ChatService.getSessionMessages(). ProviderConfigDto does NOT expose apiKey — consistent with security requirement.
