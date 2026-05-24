# LLM 流式输出的 Markdown 段落渲染方案

## 1. 大模型流式输出原理

```
用户输入 → 后端 → ChatService.streamResponse()
  → model.generate(history, handler)
    → onNext(token) → SSE event:token → 前端 onToken(token)
    → onComplete()  → SSE event:done  → 前端 onDone()
```

LLM 逐 token 生成文本。后端通过 SSE（Server-Sent Events）将每个 token 实时推送到前端，前端累积拼接后通过 `marked.js` 渲染为 HTML 并展示。

## 2. 核心问题：段落边界丢失

Spring `SseEmitter` 在发送含 `\n` 的 token 时，会按 SSE 协议拆成多行：

```
LLM 输出: "\n\n"（段落分隔）

SseEmitter 发送:
  event:token
  data:           ← 空内容，对应第一个 \n
  data:           ← 空内容，对应第二个 \n
  data:           ← SSE 事件边界（\n\n 末尾）
```

**旧解析器的处理方式：**

```javascript
const data = line.slice(5).trim()  // 空串 trim 后仍为空
onToken(data)                       // 传入空字符串，无效果
```

每一行 `data:` 独立处理，且使用 `.trim()` 去除了关键空白字符。多行数据的 `\n` 分隔符全程丢失，最终 `streamingContent` 中**没有段落边界**。

**后果：**

`marked.parse()` 收到一整段无缝文本，无法识别标题（`##`）、列表（`-`）、表格等需要前置空行的 Markdown 语法，全部渲染为连续段落。用户看到未渲染的原始格式。

重进对话时，MongoDB 中存储的完整文本段落边界完好，`marked.parse()` 一次解析成功，渲染正常。这就是"需要重新进入对话才能看到格式"的根本原因。

## 3. 解决方案

**核心思路：累积多行数据，用 `\n` 重建原始 token。**

```javascript
// 跨 chunk 持久化的状态
let eventType = null
let dataLines = []

// 逐行解析
for (const line of lines) {
  if (line.startsWith('event:')) {
    flush()                              // 事件类型切换，清空前序数据
    eventType = line.slice(6).trim()
  } else if (line.startsWith('data:')) {
    dataLines.push(line.slice(5))        // 保留原始内容，不用 .trim()
  } else if (line === '' && dataLines.length > 0) {
    flush()                              // 空行 = SSE 事件边界
  }
}

function flush() {
  const data = dataLines.join('\n')      // 用 \n 拼接重建原始 token
  if (eventType === 'token') onToken(data)
  dataLines = []
  eventType = null
}
```

**关键改动：**

| 旧方案 | 新方案 |
|--------|--------|
| 每行 `data:` 独立调 `onToken()` | 累积所有连续 `data:` 行 |
| `.trim()` 去除空白 | `slice(5)` 保留完整内容 |
| 丢失多行间 `\n` 分隔 | `join('\n')` 重建原始字符串 |
| 事件边界无法跨 chunk | `eventType`/`dataLines` 跨 `read()` 持久化 |

**效果验证（以 `\n\n` 为例）：**

```
SSE 原始数据:
  data:
  data:
  data:

dataLines = ["", "", ""]
flush → join('\n') → "\n\n"
onToken("\n\n")     ← 段落分隔完整保留
```

## 4. 辅助优化

- **始终使用 `safeMarkdown()` 渲染**：流式期间不区分渲染模式，消除"纯文本→Markdown"过渡切换的响应式同步问题
- **`safeMarkdown` 多层回退**：全文解析失败 → 逐段解析 → HTML 转义兜底
- **`try-finally` 保护 `isStreaming` 状态**：确保异常路径也重置输入框禁用状态
- **Promise 包装流读取**：`await` 仅在流完全结束后 resolve，避免 `finally` 提前执行
