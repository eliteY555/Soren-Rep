# LLM 流式输出的 Markdown 段落渲染方案

> 覆盖：后端 SSE 推送 → 前端 ReadableStream 解析 → Vue 响应式累积 → Markdown 渲染 → 打字机效果

---

## 1. 端到端数据流总览

```
┌─────────────────────────────────────────────────────────────────────────┐
│  后端 (Java / Spring Boot / LangChain4j)                                │
│                                                                         │
│  ChatService.streamResponse()                                           │
│    → StreamingChatLanguageModel.generate(history, handler)               │
│      → handler.onNext(token)   ← 逐个 token 回调，每次 1~10 字符          │
│        → SseEmitter.send(event: "token", data: token)                   │
│      → handler.onComplete()                                             │
│        → SseEmitter.send(event: "done", data: "[DONE]")                 │
│        → SseEmitter.complete()                                          │
│                                                                         │
│  关键点：SseEmitter.send() 是同步非阻塞写入，                          │
│          token 立即通过 HTTP 响应流推送到客户端                            │
└───────────────────────┬─────────────────────────────────────────────────┘
                        │ HTTP SSE (text/event-stream)
                        │ Transfer-Encoding: chunked
                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  前端 (api.js — sendMessageStream)                                      │
│                                                                         │
│  fetch('/api/chat/send', { method: 'POST', body: request })             │
│    → response.body.getReader()       ← ReadableStream 逐 chunk 读取      │
│    → TextDecoder.decode(chunk, { stream: true })                        │
│    → 按 \n 拆行 → 累积 data: 行 → 用 \n 拼接重建原始 token                │
│    → onToken(data) 回调                                                │
│    → 读到 done 事件 → onDone() 回调 / Promise resolve                    │
└───────────────────────┬─────────────────────────────────────────────────┘
                        │ onToken(token)
                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  前端 (chatStore.js — sendMessage)                                      │
│                                                                         │
│  onToken(token):                                                        │
│    streamingContent += token                   ← 累积完整文本            │
│    messages[lastIndex].content = streamingContent  ← 原地更新数组        │
│                                                                         │
│  onDone():                                                              │
│    messages[lastIndex].streaming = false        ← 移除打字机光标          │
│    loadSessions()                               ← 刷新会话列表标题        │
└───────────────────────┬─────────────────────────────────────────────────┘
                        │ Vue reactivity (Pinia ref → computed)
                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  前端 (MessageBubble.vue — renderedContent)                             │
│                                                                         │
│  computed: safeMarkdown(content)                                        │
│    → marked.parse(content)                     ← 全局配置：breaks + gfm   │
│    → hljs 代码高亮 + 自定义 codeBlock 扩展                               │
│    → try-catch 多层回退（全文 → 逐段 → HTML 转义）                       │
│    → v-html 渲染到 DOM                                                  │
│                                                                         │
│  streaming === true → <span class="typing-cursor">|</span>              │
│    CSS: animation blink 0.8s step-end infinite                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 后端：SSE 推送链路

### 2.1 ChatController — SseEmitter 端点

```java
// ChatController.java
@PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter sendMessage(@RequestBody ChatRequest request) {
    SseEmitter emitter = new SseEmitter(0L);  // 0 = 无超时

    executor.execute(() -> {
        chatService.streamResponse(request,
            token -> emitter.send(SseEmitter.event()
                .name("token").data(token)),       // 每个 token 一次 send
            () -> {
                emitter.send(SseEmitter.event()
                    .name("done").data("[DONE]"));
                emitter.complete();                 // 关闭 SSE 连接
            },
            emitter::completeWithError
        );
    });

    return emitter;  // Spring 自动设置 Content-Type: text/event-stream
}
```

### 2.2 ChatService — LangChain4j 流式回调

```
model.generate(history, handler)
  → handler.onNext("你")     → SSE: token: 你
  → handler.onNext("好")     → SSE: token: 好
  → handler.onNext("，")     → SSE: token: ，
  → handler.onNext("\n\n")   → SSE: token: \n\n  ← 段落分隔
  → handler.onNext("## 标题") → SSE: token: ## 标题
  → handler.onNext("\n\n")   → SSE: token: \n\n
  → handler.onComplete()     → SSE: done: [DONE]
```

**关键点：**
- LangChain4j 的 `StreamingResponseHandler.onNext(String token)` 每次回调传递 1 到十几个字符
- 后端不做任何拼接或缓冲，token 直接透传给 SSE
- SseEmitter 在独立线程执行，HTTP 连接保持打开直到 `complete()` 或超时
- 并发控制：`ExecutorService executor = Executors.newCachedThreadPool()` — 每个 SSE 请求占用一个线程

### 2.3 SseEmitter 的副作用：段落边界碎裂

Spring `SseEmitter` 在发送含 `\n` 的 token 时，遵循 SSE 协议规范进行多行拆分：

```
LLM 输出 token: "\n\n" (两个换行 = Markdown 段落分隔符)

SseEmitter 实际发送的 HTTP 响应体:
  event:token\n
  data:\n           ← 第一个 \n → 空内容行
  data:\n           ← 第二个 \n → 空内容行
  \n                ← SSE 事件终止符（空行）
```

这意味着 **LLM 输出的 `\n\n` 被拆分成了3行 SSE 数据**：每个 `data:` 行包含空字符串。如果前端逐行处理且不做跨行累积，段落边界就丢失了。

---

## 3. 核心问题与解决方案：SSE 多行数据解析

### 3.1 旧方案的问题

```javascript
// ❌ 旧方案：逐行独立处理，丢失多行间的 \n
for (const line of lines) {
  if (line.startsWith('data:')) {
    const data = line.slice(5).trim()   // 空串 trim 后仍为空
    if (data === '[DONE]') onDone()
    else onToken(data)                   // 传入空字符串，无效果
  }
}
```

**根因：**
- 每个 `data:` 行独立调用 `onToken()`
- `.trim()` 去除了关键的空白字符
- 多行 `data:` 之间的 `\n` 分隔符全程丢失
- 最终的 `streamingContent` 中没有段落边界

### 3.2 新方案：跨行累积 + 延迟 flush

```javascript
// ✅ 新方案：api.js sendMessageStream()
// 关键状态变量，跨 chunk 持久化
let eventType = null   // 当前事件类型："token" | "done"
let dataLines = []     // 累积当前事件的所有 data: 行内容
let finished = false   // 流是否已结束（防止重复 onDone）

function flush() {
  if (dataLines.length === 0 || !eventType) return
  const data = dataLines.join('\n')   // ← 用 \n 重建原始 token
  if (eventType === 'token') {
    onToken(data)
  } else if (eventType === 'done' || data === '[DONE]') {
    finished = true
    onDone()
    resolve()
  }
  dataLines = []
  eventType = null
}

function read() {
  if (finished) return
  reader.read().then(({ done, value }) => {
    if (finished) return
    if (done) { onDone(); resolve(); return }
    
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''  // 保留不完整的最后一行，拼入下一次 read
    
    for (const line of lines) {
      if (line.startsWith('event:')) {
        flush()                          // 事件类型切换 → 清空前序数据
        eventType = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5))    // ← 保留原始内容，不用 .trim()
      } else if (line === '' && dataLines.length > 0) {
        flush()                          // 空行 = SSE 事件边界
      }
    }
    read()  // 递归读取下一个 chunk
  })
}
read()
```

### 3.3 新旧方案对比

| 维度 | 旧方案 | 新方案 |
|------|--------|--------|
| 解析策略 | 每行 `data:` 独立调 `onToken()` | 累积所有连续 `data:` 行后一次性调用 |
| 空白处理 | `slice(5).trim()` | `slice(5)` 保留完整内容 |
| 多行拼接 | 无（丢失 `\n` 分隔） | `dataLines.join('\n')` 重建原始字符串 |
| 跨 chunk 状态 | 不持久化 | `eventType`/`dataLines`/`finished` 跨 `read()` 保持 |
| 重复回调 | 无防护 | `finished` flag + 双重 done 检测 |

### 3.4 效果验证

以 `\n\n` 为例：

```
SSE 原始 HTTP 响应:
  event:token
  data:
  data:
  data:
                              ← 空行 = SSE 事件边界

解析过程:
  line 1: "event:token" → eventType = "token"
  line 2: "data:"       → dataLines = [""]
  line 3: "data:"       → dataLines = ["", ""]
  line 4: "data:"       → dataLines = ["", "", ""]
  line 5: "" (空行)     → flush()
    → dataLines.join('\n') → "\n\n"
    → onToken("\n\n")      ← 段落分隔完整保留 ✅
```

---

## 4. 前端前端渲染：流式 Markdown 与打字机效果

### 4.1 chatStore 中的流式状态管理

```javascript
// chatStore.js — sendMessage()
async function sendMessage(content, mode, providerId) {
  // 1. 立即添加用户消息到消息列表
  messages.value = [...messages.value, 
    { role: 'USER', content, timestamp: new Date().toISOString() }]

  // 2. 立即添加空的 AI 占位消息，streaming: true
  messages.value = [...messages.value,
    { role: 'ASSISTANT', content: '', timestamp: new Date().toISOString(), streaming: true }]
  
  const aiIndex = messages.value.length - 1  // 锁定索引，防止并发问题

  isStreaming.value = true
  streamingContent.value = ''

  try {
    await api.sendMessageStream(request, {
      onToken(token) {
        streamingContent.value += token                // 追加到累积文本
        const arr = [...messages.value]                // 不可变更新，触发 Vue 响应式
        arr[aiIndex] = { ...arr[aiIndex], content: streamingContent.value }
        messages.value = arr
      },
      onDone() {
        const arr = [...messages.value]
        arr[aiIndex] = { ...arr[aiIndex], content: streamingContent.value, streaming: false }
        messages.value = arr
        loadSessions()                                 // 后台刷新会话列表
      },
      onError(err) {
        const arr = [...messages.value]
        arr[aiIndex] = { ...arr[aiIndex], content: '请求失败: ' + err.message, streaming: false }
        messages.value = arr
      }
    })
  } finally {
    isStreaming.value = false
    streamingContent.value = ''
  }
}
```

**设计要点：**
- **占位消息**：AI 回复在第一个 token 到达前就已插入消息列表，用户立即看到 "思考中…" 占位符
- **不可变更新**：`[...messages.value]` 创建新数组引用，触发 Vue 响应式更新
- **`try-finally`**：保证异常路径也重置 `isStreaming`，输入框不会被永久禁用
- **`await` 流 Promise**：`sendMessageStream` 返回的 Promise 仅在流完全结束后 resolve，`finally` 不会提前执行

### 4.2 MessageBubble — 内容渲染

```vue
<!-- MessageBubble.vue -->
<template>
  <div class="msg-content" ref="contentRef">
    <span v-html="renderedContent" />
    <span v-if="streaming" class="typing-cursor" />
  </div>
</template>

<script setup>
const renderedContent = computed(() => {
  if (!props.content) return '<span class="streaming-placeholder">思考中…</span>'
  return safeMarkdown(props.content)    // ← 全文 Markdown 解析
})
</script>
```

**两个关键视觉元素：**

| 元素 | 出现时机 | 视觉效果 |
|------|---------|---------|
| `思考中…` 占位符 | `content === ''` 时（第一个 token 到达前） | 灰色斜体文字 |
| `typing-cursor` | `streaming === true` 时 | 闪烁的 `|` 光标 |

**打字机光标的 CSS 动画：**

```css
.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 18px;
  background: var(--accent);          /* 绿色主题色 */
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 0.8s step-end infinite;
  border-radius: 1px;
}
@keyframes blink {
  50% { opacity: 0; }
}
```

### 4.3 safeMarkdown — 容错渲染

```javascript
// marked-setup.js
export function safeMarkdown(content) {
  if (!content) return ''
  try {
    return marked.parse(content)       // 主路径：全文 Markdown → HTML
  } catch (e) {
    try {
      // 回退1：按 \n\n 分段落逐段解析
      const parts = content.split(/\n\n/)
      return parts.map(p => {
        try { return marked.parse(p) }
        catch { return '<p>' + escapeHtml(p) + '</p>' }
      }).join('')
    } catch {
      // 回退2：全量 HTML 转义兜底
      return escapeHtml(content).split('\n\n').map(p => `<p>${p}</p>`).join('')
    }
  }
}
```

**三层防护：**
1. **全文解析** — 正常路径，99% 的情况走这里
2. **逐段解析** — 某段 Markdown 异常时不影响其他段落
3. **纯文本兜底** — 绝对不崩，HTML 转义后显示原文

### 4.4 流式过程中的 Markdown 渲染策略演变

项目经历了多轮迭代后，最终采用的策略是 **始终全文解析**：

| 迭代 | 策略 | 问题 |
|------|------|------|
| v1 | 流式中用纯文本，结束后切 Markdown | `v-html` / `v-text` 切换导致响应式同步问题 |
| v2 | `safeStreamingMarkdown()` — 仅渲染完整段落 | 过度复杂，且段落边界判断不稳定 |
| v3（当前） | 始终使用 `safeMarkdown()` | 简洁、一致，`marked.parse()` 对不完整 Markdown 有自然容错 |

**为什么全文解析可行？** `marked` 对未闭合的格式标记（如 `**text`、`` `code`）有内置容错，在大多数情况下不会崩溃。即便崩溃，`safeMarkdown` 的 `try-catch` 多层回退兜底。

---

## 5. 打字机效果的完整时间线

```
T0: 用户点击发送
  ├── chatStore.sendMessage() 被调用
  ├── messages 添加 USER 消息 + AI 空占位（streaming: true）
  ├── MessageBubble 渲染："思考中…"（灰色斜体）
  └── isStreaming = true → 输入框禁用

T1: 第一个 token 到达 (~200ms 首 token 延迟)
  ├── onToken("你") → streamingContent = "你"
  ├── messages 更新 → MessageBubble 重新渲染
  ├── 内容区："你|"  （typing-cursor 闪烁）
  └── 占位符消失

T2~Tn: 持续流式接收 (~30-100ms 间隔)
  ├── 每个 token 追加到 streamingContent
  ├── 每次更新触发 Vue 重新渲染
  ├── safeMarkdown 实时解析累积文本 → v-html 注入 DOM
  └── typing-cursor 始终在文本末尾闪烁

Tn: 流结束
  ├── onDone() → streaming: false
  ├── typing-cursor 消失
  ├── isStreaming = false → 输入框恢复
  ├── loadSessions() → 刷新侧边栏会话列表标题
  └── 消息列表显示完整 Markdown 渲染结果
```

---

## 6. 边界情况与防御措施

### 6.1 SSE 解析层

| 问题 | 防御 |
|------|------|
| SSE chunk 在事件中间截断 | `buffer` 保存不完整行，拼入下一次 `read()` |
| 后端异常关闭连接 | `reader.read()` 返回 `done: true` → 调用 `onDone()` |
| 重复 `[DONE]` 事件 | `finished` flag（第 11 行）— 后到的事件直接忽略 |
| 网络错误 | `.catch()` 只触发一次（`finished` flag 防护） |

### 6.2 状态管理层

| 问题 | 防御 |
|------|------|
| 流期间切换会话 | `sendMessage` 在函数入口已锁定 `currentSessionId`，后续 `onToken` 不受影响 |
| `onDone` 不在主线程 | Vue 响应式更新是同步的，`messages.value = arr` 立即生效 |
| 并发发送多条消息 | Pinia store 方法不是异步安全的 — 前端 UI 通过 `isStreaming` 禁用发送按钮 |
| `finally` 提前执行 | `sendMessageStream` 返回的 Promise 仅在流结束后 resolve（第 22 行 `new Promise`） |

### 6.3 渲染层

| 问题 | 防御 |
|------|------|
| 不完整 Markdown 导致 parse 报错 | `safeMarkdown` 三层 `try-catch` 回退 |
| 代码块中包含特殊字符 | `data-code` 属性使用 `&amp;`/`&lt;`/`&quot;` 转义 |
| 长时间流式期间 DOM 堆积 | `marked.parse()` 每次返回完整 HTML 字符串，`v-html` 全量替换，DOM 大小恒定 |

---

## 7. 与市场方案的对比

| 方案 | 本项目 | ChatGPT | 豆包 |
|------|--------|---------|------|
| 传输协议 | SSE (text/event-stream) | SSE | WebSocket |
| 流式解析 | 手动 ReadableStream + SSE 解析器（~80 行） | 封装良好 | 封装良好 |
| 累积策略 | 前端 `streamingContent += token` | 前端累积 | 前端累积 |
| Markdown 渲染 | 流式中全文 `marked.parse()` | 流式→最终一致 | 流式→最终一致 |
| 打字机光标 | CSS `blink` 动画 + `v-if="streaming"` | 无（逐词渲染足够快） | 无 |
| 未完毕落容错 | safeMarkdown 三层回退 | 内置 | 内置 |

---

## 8. 关键性能指标

| 指标 | 目标 | 现状 |
|------|------|------|
| 首 token 延迟 | < 2s | ~200-500ms（取决于 LLM API 响应） |
| token 间延迟 | < 100ms | ~30-80ms |
| Vue 渲染帧率 | 不掉帧 | 每个 token 触发一次 computed 重算 + DOM update |
| 内存占用 | 稳定无泄漏 | streamingContent 最长约 4KB（一次回答），流结束后清空 |
| SSE 超时 | 120s（LangChain4j 默认） | 配置了 `SseEmitter(0L)` 无超时，LLM 侧 `Duration.ofSeconds(120)` |
