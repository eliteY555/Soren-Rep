<p align="center">
  <h1 align="center">🏥 诊易通 (YZT-Medicine)</h1>
  <p align="center"><strong>AI 驱动的智能在线问诊平台</strong></p>
  <p align="center">
    <img src="https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen?logo=springboot" alt="Spring Boot">
    <img src="https://img.shields.io/badge/Vue-2.6.14-4FC08D?logo=vuedotjs" alt="Vue">
    <img src="https://img.shields.io/badge/Java-17+-ED8B00?logo=openjdk" alt="Java">
    <img src="https://img.shields.io/badge/LangChain4j-1.0.0--beta3-blue" alt="LangChain4j">
    <img src="https://img.shields.io/badge/DeepSeek-V4--Pro-536DFE" alt="DeepSeek">
    <img src="https://img.shields.io/badge/MySQL-8.x-4479A1?logo=mysql" alt="MySQL">
    <img src="https://img.shields.io/badge/MongoDB-7.x-47A248?logo=mongodb" alt="MongoDB">
    <img src="https://img.shields.io/badge/Pinecone-Vector%20DB-000000" alt="Pinecone">
    <img src="https://img.shields.io/badge/license-MIT-blue" alt="License">
  </p>
</p>

---

## 📖 项目简介

**诊易通**是一款 AI 驱动的智能在线问诊平台，整合**大语言模型对话**、**Function Calling 自动建病历**、**RAG 向量知识检索**、**会话记忆**四大 AI 工程能力，连接患者与医生，提供从 AI 智能导诊 → 医生诊断 → 评分反馈的完整闭环诊疗服务。

> 💡 **核心价值**：患者通过 AI 助手"医疗小易"口语化描述症状，AI 自动提取关键信息、生成结构化病历、推荐科室医生；医生在线接诊、给出诊断处方；患者查看结果并评分交流——形成完整的在线医疗流程。

---

## 🏗️ 系统架构

```
┌────────────────────────────────────────────────────────────────┐
│                     Vue 2.x SPA 前端 (web/)                     │
│   登录/注册 → AI 问诊 / 病历管理 / 医生列表 / 评论交流 / 数据统计  │
└───────────────────────────┬────────────────────────────────────┘
                            │  HTTP REST + SSE Streaming
┌───────────────────────────▼────────────────────────────────────┐
│                Spring Boot 3.5.0 后端 (medicine/)               │
│                                                                 │
│   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│   │   Auth   │ │ Patient  │ │  Record  │ │   Agent (AI)     │  │
│   │Controller│ │Controller│ │Controller│ │   Controller     │  │
│   └────┬─────┘ └────┬─────┘ └────┬─────┘ └───────┬──────────┘  │
│        │             │             │               │             │
│   ┌────▼─────────────▼─────────────▼───────────────▼─────────┐  │
│   │                     Service 层                            │  │
│   │  PatientService / DoctorService / RecordService           │  │
│   │  DiagnosticService / CommentService / ReplyService        │  │
│   └──────┬────────────────────────────────────┬──────────────┘  │
│          │                                     │                 │
│   ┌──────▼──────┐                    ┌─────────▼──────────┐     │
│   │   MyBatis   │                    │   LangChain4j AI   │     │
│   │   Mapper    │                    │   ┌──────────────┐ │     │
│   └──────┬──────┘                    │   │AgentAssistant │ │     │
│          │                           │   │  @AiService   │ │     │
│          │                           │   ├──────────────┤ │     │
│          │                           │   │ AgentTools    │ │     │
│          │                           │   │FuncCalling    │ │     │
│          │                           │   ├──────────────┤ │     │
│          │                           │   │ MemoryStore   │ │     │
│          │                           │   │  (MongoDB)    │ │     │
│          │                           │   ├──────────────┤ │     │
│          │                           │   │ContentRetrieve│ │     │
│          │                           │   │  (Pinecone)   │ │     │
│          │                           │   └──────────────┘ │     │
│          │                           └──┬────┬──────┬─────┘     │
└──────────┼───────────────────────┼─────┼──────┼───────────────┘
           │                       │     │      │
      ┌────▼────┐            ┌─────▼──┐ ┌▼──────▼──┐
      │  MySQL  │            │MongoDB │ │ DeepSeek │
      │medicine │            │Chat Mem│ │   API    │
      └─────────┘            └────────┘ └──────────┘
                                  │
                              ┌───▼──────┐
                              │ Pinecone │
                              │Vector DB │
                              └──────────┘
```

---

## 🛠️ 技术栈

### 后端

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.5.0 |
| ORM | MyBatis | 3.0.3 |
| 数据库 | MySQL | 8.x |
| 文档数据库 | MongoDB | 7.x |
| 向量数据库 | Pinecone | AWS us-east-1 |
| AI 框架 | LangChain4j | 1.0.0-beta3 |
| 大语言模型 | DeepSeek V4-Pro | — |
| 嵌入模型 | 阿里云百炼 text-embedding-v3 | — |
| 流式响应 | Spring WebFlux + Reactor | 3.5.0 |
| 加密 | BouncyCastle + Spring Security Crypto | — |
| 文档解析 | Apache PDFBox | — |
| 代码简化 | Lombok | 1.18.32 |
| API 文档 | Knife4j | 4.3.0 |

### 前端

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue | 2.6.14 |
| UI 组件库 | Element UI | 2.15.14 |
| 状态管理 | Vuex | 3.6.2 |
| 路由 | Vue Router | 3.5.1 |
| HTTP 客户端 | Axios | 1.7.9 |
| 图表 | ECharts | 5.6.0 |
| Markdown 渲染 | marked | 18.0.4 |
| 日期处理 | dayjs | 1.11.13 |
| 前端加密 | crypto-js | 4.2.0 |
| CSS 预处理器 | Sass | 1.83.4 |

---

## 🚀 快速启动

### 环境要求

- **JDK** 17+
- **Maven** 3.8+
- **MySQL** 8.0+
- **MongoDB** 7.0+
- **Node.js** 16+ (前端)

### 1. 克隆项目

```bash
git clone https://github.com/eliteY555/Soren-Rep.git
cd Soren-Rep
git checkout YZT-medicine
```

### 2. 配置数据库

#### MySQL

创建数据库并初始化表结构：

```sql
CREATE DATABASE IF NOT EXISTS medicine DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

启动后端后，JPA 会自动建表（`ddl-auto: update`）。

#### MongoDB

确保 MongoDB 服务运行在 `localhost:27017`，数据库 `chat_memory_db` 会自动创建。

### 3. 配置环境变量

在启动后端之前，需要设置以下环境变量：

| 环境变量 | 必填 | 说明 |
|----------|------|------|
| `DB_USERNAME` | 否 | MySQL 用户名（默认 `root`） |
| `DB_PASSWORD` | 否 | MySQL 密码（默认 `2003`） |
| `DEEPSEEK_KEY` | ✅ 是 | DeepSeek API Key |
| `QWEN_KEY` | 否 | 阿里云百炼 API Key（用于文本嵌入） |
| `PINECONE_KEY` | 否 | Pinecone API Key（用于 RAG 向量检索） |

### 4. 启动后端

```bash
cd medicine
mvn spring-boot:run
```

后端运行在 **http://localhost:9999**

### 5. 启动前端

```bash
cd web
npm install
npm run serve
```

前端开发服务器运行在 **http://localhost:8080**

### 6. 访问应用

- 前端页面：http://localhost:8080
- 内嵌聊天页：http://localhost:9999/index.html
- 默认配置的用户可直接在登录页注册

---

## 📁 项目结构

```
YZT-Medicine/
├── medicine/                          # Spring Boot 后端
│   ├── src/main/java/com/me/
│   │   ├── MedicineApplication.java   # 应用入口
│   │   ├── assistant/                 # AI 助手层
│   │   │   └── AgentAssistant.java    # LangChain4j @AiService 接口 (流式对话)
│   │   ├── tools/                     # AI 工具层
│   │   │   └── AgentTools.java        # Function Calling 工具 (自动建病历)
│   │   ├── store/                     # 存储层
│   │   │   └── MemoryStore.java       # MongoDB 会话记忆实现
│   │   ├── controller/                # REST 控制器层
│   │   │   ├── AgentController.java   # AI 聊天接口 (SSE 流式)
│   │   │   ├── AuthController.java    # 统一认证接口
│   │   │   ├── PatientController.java # 患者管理接口
│   │   │   ├── DoctorController.java  # 医生管理接口
│   │   │   ├── RecordController.java  # 病历管理接口
│   │   │   ├── CommentController.java # 评论管理接口
│   │   │   └── ReplyController.java   # 回复管理接口
│   │   ├── service/                   # 业务逻辑接口
│   │   │   └── impl/                  # 业务逻辑实现
│   │   │       ├── DiagnosticServiceImpl.java  # 含 Jaccard 诊断推荐算法
│   │   │       ├── PatientServiceImpl.java
│   │   │       ├── DoctorServiceImpl.java
│   │   │       ├── RecordServiceImpl.java      # 含 @Transactional 病历+诊断事务
│   │   │       ├── CommentServiceImpl.java
│   │   │       └── ReplyServiceImpl.java
│   │   ├── mapper/                    # MyBatis Mapper 接口
│   │   ├── pojo/                      # 数据模型 / DTO
│   │   │   ├── Patient.java           # 患者 (含登录凭证+健康档案)
│   │   │   ├── Doctor.java            # 医生 (含登录凭证+执业信息)
│   │   │   ├── Record.java            # 病历 (含状态流转)
│   │   │   ├── Diagnostic.java        # 诊断 (含处方/医嘱)
│   │   │   ├── Comment.java           # 评论 (含嵌套回复)
│   │   │   └── ...
│   │   ├── config/                    # Spring 配置
│   │   │   ├── CommonConfig.java      # ChatMemoryProvider + ContentRetriever
│   │   │   ├── EmbeddingStoreConfig.java  # Pinecone 向量库配置
│   │   │   └── JacksonConfig.java     # 双 ObjectMapper (含 LangChain4j 兼容)
│   │   ├── common/                    # 通用工具
│   │   │   ├── Result.java            # 统一响应封装
│   │   │   └── ResultEnum.java        # 错误码枚举
│   │   └── utils/
│   │       ├── PasswordUtil.java      # AES 密码工具
│   │       └── CustomDateDeserializer.java  # 多格式日期解析
│   ├── src/main/resources/
│   │   ├── application.yml            # 应用配置
│   │   ├── SystemPrompt.txt           # AI 系统提示词 ("医疗小易" 人设)
│   │   ├── mapper/                    # MyBatis XML 映射文件
│   │   └── static/
│   │       └── index.html             # 内嵌聊天页面
│   └── pom.xml
├── web/                               # Vue 2.x 前端
│   ├── src/                           # Vue 源码
│   │   ├── views/                     # 页面组件
│   │   ├── components/                # 公共组件
│   │   ├── router/                    # 路由配置
│   │   ├── store/                     # Vuex 状态管理
│   │   ├── utils/                     # 工具函数 (streamParser.js, secret.js)
│   │   └── api/                       # API 封装
│   ├── dist/                          # 构建产物
│   └── package.json
├── docs/
│   └── 项目业务逻辑梳理.md              # 详细业务逻辑文档
└── README.md
```

---

## 🔑 核心功能

### 1. 🤖 AI 智能问诊

基于 DeepSeek V4-Pro 大语言模型，打造智能助手**"医疗小易"**：

```
患者: "最近有点头疼发热"
  ↓
医疗小易 (流式响应): "您好！根据您的描述，我来帮您进一步了解情况..."
  ↓ AI 自动提取信息、触发 Function Calling
  ↓ 自动创建结构化病历 → 写入 MySQL
  ↓
推荐科室/医生 → 进入医生接诊流程
```

**AI 层技术亮点：**

| 能力 | 实现 | 技术 |
|------|------|------|
| 流式对话 | SSE (Server-Sent Events) 逐字推送 | Spring WebFlux + `Flux<String>` |
| 自动建病历 | AI 从对话中提取字段 → 调用 `AgentTools.upLoadRecord()` | LangChain4j `@Tool` Function Calling |
| 会话记忆 | 保留最近 20 轮对话，跨请求连续交流 | MongoDB ChatMemoryStore + `MessageWindowChatMemory` |
| RAG 检索 | 从 Pinecone 向量库检索相关医学知识增强回答 | `EmbeddingStoreContentRetriever` (maxResults=1, minScore=0.8) |
| 系统提示词 | 结构化设计"医疗小易"人设、流程、边界 | `SystemPrompt.txt`（身份定位 / 医疗咨询 / 导诊 / 病例流程 / 交流技巧 / 边界控制） |

### 2. 👤 统一认证系统

- **无独立 user 表**：登录凭证直接存储在 `patient` 和 `doctor` 表中
- **统一入口**：`POST /auth/login` 以手机号为标识，自动判断患者/医生身份
- **密码安全**：前端 AES 加密传输 → 后端 BCrypt 验证（传输层加密 + 存储层哈希双层保护）
- **角色体系**：role=0 (患者) / role=1 (医生)

### 3. 📋 病历全生命周期管理

```
状态1「待诊断」→ 状态2「已诊断」→ 状态3「已完成」
 (患者/AI 提交)    (医生填写中)      (可评分评论)
```

- **事务保证**：`@Transactional` 确保诊断更新与病历状态变更的原子性
- **诊断 UPSERT**：状态2 下医生可反复修改诊断，状态3 后固定
- **权限控制**：患者只能查看自己的病历，医生只能查看自己接诊的病历
- **相似诊断推荐**：基于 Jaccard 相似度算法，从历史诊断库中匹配最相似的病例

### 4. 👨‍⚕️ 医生资源管理

- 按城市 / 医院 / 科室多维度筛选医生
- UPSERT 模式创建/更新医生信息（`INSERT ... ON DUPLICATE KEY UPDATE`）
- 分页查询 + 灵活筛选
- 患者评分体系支撑医生口碑排行

### 5. 💬 评论回复系统

- 病历完成后支持评论 + 嵌套回复
- MyBatis `<resultMap>` + `<collection>` 实现一次 JOIN 查询获取评论及所有回复
- 支持患者-医生双向互动交流

### 6. 📊 数据可视化

- ECharts 驱动的数据统计面板
- 就诊趋势、科室分布、医生工作量等多维度分析

---

## 📡 API 接口概览

### 认证模块 `/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 统一登录 (identity + password) |
| GET | `/auth/findPassword` | 通过手机号查找用户 |
| POST | `/auth/update` | 修改账户资料 |

### 患者模块 `/patient`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/patient/register` | 患者注册（含健康档案） |
| POST | `/patient/create` | 创建患者健康档案 |
| POST | `/patient/update` | 更新患者健康档案 |
| GET | `/patient/get/{userId}` | 获取患者信息 |

### 医生模块 `/doctor`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/doctor/register` | 医生注册（含执业信息） |
| POST | `/doctor/create` | 创建医生信息 (UPSERT) |
| POST | `/doctor/update` | 更新医生信息 (UPSERT) |
| GET | `/doctor/get/{doctorId}` | 获取医生信息 |
| GET | `/doctor` | 查询全部医生 |
| POST | `/doctor/page` | 分页筛选医生 |

### 病历模块 `/record`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/record/add` | 创建病历 (status=1) |
| POST | `/record/page` | 分页查询病历 |
| GET | `/record/{recordId}` | 病历详情（含诊断+医生信息） |
| POST | `/record/update` | 更新病历（事务：病历状态 + 诊断 UPSERT） |
| POST | `/record/recommend` | Jaccard 相似诊断推荐 |

### AI 聊天模块 `/agent`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/agent/chat` | AI 流式对话（SSE，`text/stream;charset=utf-8`） |

### 评论模块 `/comment` & `/reply`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/comment` | 发表评论 |
| GET | `/comment/{recordId}` | 查病历评论（含嵌套回复） |
| POST | `/reply/add` | 添加回复 |
| DELETE | `/reply/delete/{replyId}` | 删除回复 |

> 所有接口统一返回格式：`{"code": 200, "msg": "操作成功", "result": {...}}`

---

## 🗄️ 数据库设计

### ER 关系图

```
┌──────────┐        ┌──────────┐        ┌──────────┐
│ patient  │        │  record  │        │  doctor  │
├──────────┤ 1:N    ├──────────┤ N:1    ├──────────┤
│patientId │────────│patientId │     ┌──│doctorId  │
│patientName       │doctorId  │─────┘  │doctorName│
│username  │        │description│       │username  │
│password  │        │status    │        │password  │
│phone     │        │score     │        │phone     │
│oldHistory│        │createTime│        │cityName  │
│allergies │        └──────────┘        │hospital  │
│habits    │              │             │department│
└──────────┘              │1:1          └──────────┘
              ┌──────────┐│
              │diagnostic││
              ├──────────┤│
              │recordId  │┘
              │result    │
              │prescriptn│
              │orders    │
              └──────────┘
              ┌──────────┐       ┌──────────┐
              │ comment  │       │  reply   │
              ├──────────┤ 1:N   ├──────────┤
              │recordId  │───────│commentId │
              │userId    │       │userId    │
              │role      │       │role      │
              │content   │       │content   │
              └──────────┘       └──────────┘
```

### 病历状态流转

```
  [患者/AI提交病历]
         │
         ▼
   ┌──────────┐     医生接诊     ┌──────────┐    提交诊断结果    ┌──────────┐
   │ 状态 1   │ ──────────────→ │ 状态 2   │ ───────────────→ │ 状态 3   │
   │ 待诊断   │                 │ 已诊断   │                   │ 已完成   │
   └──────────┘                 └──────────┘                   └──────────┘
                                     │                         (可评分评论)
                                     │ (医生可反复修改诊断)
                                     └──────→ 更新诊断记录
```

---

## ⚠️ 注意事项

### 安全建议

| 问题 | 现状 | 建议 |
|------|------|------|
| 密码存储 | 数据库存储明文密码 | 建议改用 BCrypt 单向哈希存储 |
| 默认密码 | AI 自动创建患者使用硬编码密码 `123456` | 建议生成随机密码并通过短信/邮件发送 |
| AES 密钥 | 密钥硬编码在代码中 | 建议通过环境变量或密钥管理服务注入 |
| SQL 注入 | MyBatis XML 部分使用 `${}` 拼接 | 建议统一使用 `#{}` 参数化查询 |

### 版本兼容性

- `pom.xml` 声明 Java 1.8，但 Spring Boot 3.5.0 要求 Java 17+，请使用 JDK 17 或更高版本
- 中文分词（Jaccard 相似度）当前使用空格分词，对中文效果有限，后续可接入专业中文分词器（如 HanLP、jieba）

---

## 🔮 未来规划

- [ ] 接入更多大语言模型（通义千问、GPT-4o 等）实现模型热切换
- [ ] 移动端适配（React Native / Flutter App）
- [ ] 药品配送功能——线上下单、配送到家
- [ ] 视频问诊——医患实时音视频通话
- [ ] 医学知识图谱——整合疾病、症状、药品关系网络
- [ ] 中医体质辨识——结合舌诊、面诊的专项 AI 模块
- [ ] 健康知识科普板块
- [ ] 引入专业中文分词提升诊断推荐准确度

---

## 📄 License

MIT License

---

<p align="center">
  <strong>© 2024 诊易通 — 让智慧医疗触手可及</strong>
</p>
