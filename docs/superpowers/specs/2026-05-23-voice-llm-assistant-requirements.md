# 语音 AI 助手 — 需求规格说明书

> 版本: v1.0 | 日期: 2026-05-23 | 状态: 待评审

---

## 1. 项目概述

### 1.1 产品定义

一款 **Web 端语音 AI 助手**，通过设备麦克风采集人声，实时转写为文本，发送给可配置的大语言模型（LLM）获取回答。支持两种提问模式：**直接问答** 和 **RAG 增强问答**（基于个人知识库检索增强后回答）。

### 1.2 目标用户

个人用户，单租户，无需登录/权限体系。

### 1.3 核心价值

- 语音输入解放双手，降低使用门槛
- RAG 模式让 LLM 基于个人私有知识库作答，提升回答准确性与可信度
- 多 LLM 提供商可切换，不绑定单一厂商

---

## 2. 功能模块

### 2.1 语音采集与识别 (STT)

| 编号 | 功能 | 描述 | 优先级 |
|------|------|------|--------|
| STT-01 | 按键说话模式 (PTT) | 按住按钮开始录音，松开后自动识别并填入输入框 | P0 |
| STT-02 | 实时流式模式 | 持续监听，自动断句，实时显示转写文本 | P1 |
| STT-03 | 模式切换 | PTT / 流式两种模式可切换，默认 PTT | P0 |
| STT-04 | 录音状态反馈 | 录音时显示波形/音量指示器、计时 | P1 |
| STT-05 | 文本手动编辑 | 识别结果可手动修改后再发送 | P0 |
| STT-06 | 浏览器兼容检查 | 检测当前浏览器是否支持 Web Speech API，不支持时给出提示 | P0 |

**技术选型**: MVP 使用浏览器内置 Web Speech API (SpeechRecognition)，预留云端 STT 扩展接口。

### 2.2 LLM 对话

| 编号 | 功能 | 描述 | 优先级 |
|------|------|------|--------|
| LLM-01 | 提供商配置 | 后台配置界面管理 LLM 提供商（apiKey, endpoint, model） | P0 |
| LLM-02 | 多提供商支持 | 支持 OpenAI、DeepSeek、通义千问、Ollama 等，通过 LangChain4j 适配 | P0 |
| LLM-03 | 提供商切换 | 用户可在前端切换当前使用的 LLM 提供商 | P0 |
| LLM-04 | 流式输出 (SSE) | LLM 回复以流式方式返回前端，打字机效果呈现 | P0 |
| LLM-05 | 对话历史 | 当前会话内保留对话上下文（多轮对话记忆） | P0 |
| LLM-06 | 模型参数调节 | 支持调节 temperature、maxTokens、topP 等参数 | P2 |
| LLM-07 | 停止生成 | 支持中途停止 LLM 回复生成 | P2 |

### 2.3 RAG 知识库

| 编号 | 功能 | 描述 | 优先级 |
|------|------|------|--------|
| RAG-01 | 文档上传 | 支持上传 PDF、TXT、Markdown、Word 文档到知识库 | P1 |
| RAG-02 | 文档管理 | 查看已上传文档列表，支持删除 | P1 |
| RAG-03 | 文档分块 (Chunking) | 上传后自动切分文档为语义块，配置块大小与重叠 | P1 |
| RAG-04 | 向量化存储 | 文本块经 Embedding 模型向量化后存入 Pinecone | P1 |
| RAG-05 | 检索增强问答 | 提问时先从 Pinecone 检索相关文档片段，拼入 Prompt 后发给 LLM | P1 |
| RAG-06 | 问答模式切换 | 每条提问可选择 "直接问答" 或 "RAG 增强"，默认直接问答 | P0 |
| RAG-07 | 检索源引用 | RAG 回答中标注引用了哪些文档的哪些片段 | P2 |
| RAG-08 | 知识库范围选择 | 提问时可指定检索全部文档或特定文档 | P2 |

### 2.4 对话管理

| 编号 | 功能 | 描述 | 优先级 |
|------|------|------|--------|
| CHAT-01 | 多会话 | 支持创建/切换/删除多个对话会话 | P1 |
| CHAT-02 | 会话重命名 | 自动使用首条提问摘要命名，支持手动修改 | P2 |
| CHAT-03 | 对话历史持久化 | 所有对话历史存储到 MongoDB，刷新页面不丢失 | P0 |
| CHAT-04 | 导出对话 | 导出对话为 Markdown 或 PDF | P2 |

### 2.5 系统配置

| 编号 | 功能 | 描述 | 优先级 |
|------|------|------|--------|
| SYS-01 | LLM 提供商 CRUD | 管理可用的 LLM 提供商配置，存储到 MySQL | P0 |
| SYS-02 | Embedding 模型配置 | 配置向量化所用的 Embedding 模型 | P1 |
| SYS-03 | 系统设置 | 默认 LLM、默认问答模式、语言偏好等 | P2 |

---

## 3. 技术架构

### 3.1 技术栈

| 层 | 技术 | 说明 |
|----|------|------|
| 前端 | Vue 3 + Vite + Pinia | SPA 单页应用 |
| UI 框架 | Element Plus | 桌面端优先 |
| 语音识别 | Web Speech API | MVP 方案，预留云端 STT 扩展 |
| 后端 | Spring Boot 3.x + Java 17+ | RESTful API + SSE 流式 |
| LLM 框架 | LangChain4j | 多模型统一调用 + RAG 管线 |
| 向量数据库 | Pinecone | 文档向量存储与相似度检索 |
| 对话存储 | MongoDB | 对话历史、会话元数据 |
| 业务数据库 | MySQL | LLM 配置、文档元数据、系统设置 |
| 构建工具 | Maven | 多模块项目 |

### 3.2 选择 LangChain4j 的理由

1. **多提供商**: 原生支持 OpenAI / DeepSeek / 千问 / Ollama / ZhipuAI 等 20+ 模型，通过统一 `ChatLanguageModel` 接口切换，无需写适配代码
2. **完整 RAG 管线**: `DocumentLoader` → `DocumentSplitter` → `EmbeddingStore` → `ContentRetriever` → `AiServices`，一条链路全部内置
3. **Pinecone 原生集成**: `langchain4j-pinecone` 模块直接对接
4. **Spring Boot Starter**: `langchain4j-spring-boot-starter` 提供自动配置，与 Spring 生态无缝整合
5. **流式支持**: 原生 `StreamingChatLanguageModel` + SSE 输出

### 3.3 模块划分

```
voice-llm-assistant/
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── components/         # 语音按钮、对话面板、配置面板
│   │   ├── composables/        # useSpeech, useChat, useRag
│   │   ├── stores/             # Pinia: chatStore, configStore
│   │   ├── views/              # 主页面
│   │   └── services/           # API 调用封装
│   └── ...
├── backend/
│   ├── voice-assistant-web/    # Controller 层，SSE 端点
│   ├── voice-assistant-service/# 业务逻辑层
│   ├── voice-assistant-llm/    # LangChain4j 配置、LLM 工厂、RAG 管线
│   ├── voice-assistant-repo/   # 数据访问层 (MySQL + MongoDB + Pinecone)
│   └── voice-assistant-common/ # 公共 DTO、工具类
└── docs/
    └── superpowers/
        └── specs/              # 需求 & 设计文档
```

### 3.4 数据存储分工

| 数据类型 | 存储 | 原因 |
|----------|------|------|
| LLM 提供商配置 | MySQL | 结构化数据，量小，需持久可靠 |
| 文档元数据（名称、大小、上传时间） | MySQL | 结构化，需关联查询 |
| 对话历史 | MongoDB | 半结构化（消息列表嵌套），写入频繁，JSON 友好 |
| 文档向量 | Pinecone | 专用向量检索，按相似度 Top-K 查询 |

### 3.5 数据流

```
┌──────────┐   语音流    ┌──────────┐   HTTP/SSE   ┌──────────────┐
│  浏览器   │ ──────────→ │  Vue 前端  │ ───────────→ │ Spring Boot   │
│  麦克风   │ ←─ Web API  │ (SPA)     │ ←─────────── │  后端         │
└──────────┘             └──────────┘              └──────┬───────┘
                                                          │
              ┌───────────────────────────────────────────┼───────────────┐
              │                                           │               │
              ▼                                           ▼               ▼
    ┌──────────────┐                          ┌──────────────┐   ┌──────────┐
    │   Pinecone   │                          │    MongoDB    │   │  MySQL   │
    │  (向量检索)   │                          │  (对话历史)   │   │ (配置)   │
    └──────────────┘                          └──────────────┘   └──────────┘
              │
              │  检索结果 + 用户问题
              ▼
    ┌──────────────┐
    │  LLM Provider │  (OpenAI / DeepSeek / 千问 / Ollama)
    │  LangChain4j  │
    └──────┬───────┘
           │ SSE 流式返回
           ▼
    ┌──────────────┐
    │   Vue 前端     │  打字机效果渲染
    └──────────────┘
```

---

## 4. 核心 API 设计

### 4.1 对话 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat/send` | 发送消息（参数: sessionId, content, mode=[direct|rag], providerId） |
| GET | `/api/chat/stream/{messageId}` | SSE 订阅流式回复 |
| GET | `/api/chat/sessions` | 获取会话列表 |
| POST | `/api/chat/sessions` | 创建新会话 |
| DELETE | `/api/chat/sessions/{id}` | 删除会话 |
| GET | `/api/chat/sessions/{id}/messages` | 获取会话历史消息 |

### 4.2 知识库 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/knowledge/documents` | 上传文档（multipart/form-data） |
| GET | `/api/knowledge/documents` | 文档列表 |
| DELETE | `/api/knowledge/documents/{id}` | 删除文档及其向量 |
| POST | `/api/knowledge/search` | 语义检索测试（调试用, 参数: query, topK） |

### 4.3 配置 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/config/providers` | LLM 提供商列表 |
| POST | `/api/config/providers` | 添加提供商配置 |
| PUT | `/api/config/providers/{id}` | 修改提供商配置 |
| DELETE | `/api/config/providers/{id}` | 删除提供商 |
| PUT | `/api/config/providers/{id}/activate` | 切换当前激活的提供商 |

---

## 5. 非功能性需求

| 类别 | 要求 |
|------|------|
| **性能** | STT 识别延迟 < 500ms；RAG 检索 + LLM 首 token 延迟 < 3s |
| **可用性** | 浏览器兼容：Chrome 90+, Edge 90+（Web Speech API 限制）；后端单机部署即可 |
| **安全** | API Key 加密存储（AES-256），不在前端传输明文；文档上传大小限制 20MB |
| **可扩展** | LLM 提供商 / Embedding 模型 / STT 引擎均通过接口抽象，新接入只需实现接口 |
| **数据** | 用户可一键删除某会话全部数据（MongoDB + 关联向量） |

---

## 6. 开发阶段规划

### Phase 1: 核心 MVP（预计 3-5 天）

- Spring Boot 项目初始化，多模块骨架
- LangChain4j 集成，支持 OpenAI + DeepSeek 两个提供商
- Vue 3 前端骨架，Web Speech API PTT 模式
- 文本对话（非 RAG），SSE 流式输出
- 对话历史存 MongoDB，会话切换

### Phase 2: RAG 增强（预计 2-3 天）

- 文档上传 + 分块 + 向量化流水线
- Pinecone 检索集成
- RAG 问答模式（检索 → 拼 Prompt → LLM 回答）

### Phase 3: 体验优化（预计 2-3 天）

- 实时流式语音模式
- UI 打磨（录音动效、打字机效果、Markdown 渲染）
- 多提供商配置 UI
- 知识库文档管理 UI

### Phase 4: 扩展完善（按需）

- 云端 STT 可选方案
- 对话导出
- 模型参数调节
- 检索源引用展示

---

## 7. 已确认事项

1. **前端 UI 组件库**: Element Plus
2. **默认 LLM 提供商**: DeepSeek V4 Pro（首次启动预置配置）
3. **本地 Ollama**: 不需要
