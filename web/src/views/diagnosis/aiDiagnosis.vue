<template>
  <div class="app-container">
    <div class="card-container">
      <div class="card-header">智能问诊</div>
      <div class="card-body">
        <div class="chat-container" ref="chatContainer">
          <div class="chat-messages" ref="chatMessages">
            <div class="message system-message">
              <div class="message-content">
                您好，我是智能问诊助手<strong>"小易"</strong>。可以帮您提交病例、解答健康问题、提供就诊建议。
              </div>
            </div>

            <div v-for="(message, index) in messages" :key="index"
                 :class="['message', message.type === 'user' ? 'user-message' : 'ai-message']">
              <div class="message-content markdown-body"
                   v-html="message._html || ''"
                   :ref="'msg-' + index"></div>
            </div>

            <!-- 流式输出气泡 — 直接 DOM 写入，绕过 Vue vdom -->
            <div v-if="streaming" class="message ai-message">
              <div class="message-content markdown-body"
                   id="stream-content"></div>
            </div>

            <div v-if="loading && !streaming" class="message ai-message">
              <div class="message-content loading-indicator">
                <span class="dot"></span><span class="dot"></span><span class="dot"></span>
              </div>
            </div>
          </div>
        </div>

        <div class="input-container">
          <el-input type="textarea" :rows="2"
            placeholder="请详细描述您的症状..."
            v-model="inputMessage"
            :disabled="loading"
            @keyup.ctrl.enter.native="sendMessage"
            resize="none" />
          <div class="button-container">
            <span class="shortcut-tip">Ctrl+Enter 发送</span>
            <el-button type="primary" :loading="loading"
              :disabled="!inputMessage.trim()" @click="sendMessage">发送</el-button>
            <el-button v-if="loading" type="warning" size="small"
              @click="stopStreaming">停止</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { marked } from 'marked'
import { parseStream } from '@/utils/streamParser'

/**
 * Streaming-markdown-solution 核心设计：
 *
 *   流式期间：纯文本渲染（\n → <br> + HTML 转义），不调 marked.parse
 *   → 每次 token 只做 cheap string replace + DOM 写回
 *   → requestAnimationFrame 批量更新 DOM，最多 60fps
 *
 *   流结束后：marked.parse() 全量解析一次，得到最终 HTML
 *
 *   SseEmitter 写入格式（后端 AgentController）：
 *     event:token\ndata:token文本\n\n
 *   当 token 内含 \n 时 SseEmitter 自动拆行：
 *     event:token\ndata:第一行\ndata:第二行\n\n
 *   → 前端 processLines 累积 data: 行 → join('\n') 重建
 */
export default {
  name: 'AiDiagnosis',
  data() {
    return {
      messages: [],
      inputMessage: '',
      loading: false,
      streaming: false,
      memoryId: null,
      controller: null,
      _streamText: '',      // 累积纯文本，避免 Vue 响应式触发 re-render
      _rafId: null          // requestAnimationFrame ID
    }
  },
  computed: {
    userInfo() { return this.$store.state.user.userInfo }
  },
  mounted() {
    this.initMemoryId()
    window.addEventListener('resize', () => this.scrollToBottom())
  },
  beforeDestroy() {
    window.removeEventListener('resize', () => this.scrollToBottom())
    if (this._rafId) cancelAnimationFrame(this._rafId)
    if (this.controller) this.controller.abort()
  },
  methods: {
    initMemoryId() {
      this.memoryId = this.userInfo?.userId
      if (!this.memoryId) this.$message.error('登录信息异常')
    },

    // === 流式渲染核心 ===

    /** token 到达：存入队列，RAF 批量 flush 到 DOM */
    _onStreamToken(token) {
      this._streamText += token
      this._scheduleRender()
    },

    /** 最多 60fps 更新 DOM，不经过 Vue reactivity */
    _scheduleRender() {
      if (this._rafId) return  // 已有待执行任务
      this._rafId = requestAnimationFrame(() => {
        this._rafId = null
        this._updateStreamDOM()
      })
    },

    /** 将累积文本直接写入 DOM（cheap：plain html escape + <br>） */
    _updateStreamDOM() {
      const el = document.getElementById('stream-content')
      if (!el) return
      el.innerHTML = plainTextToHTML(this._streamText)
      this.scrollToBottomSmooth()
    },

    /** 流结束：marked 全量解析，存入 Vue messages */
    _onStreamDone() {
      this.streaming = false
      const finalText = this._streamText
      const finalHTML = safeFullParse(finalText)

      this.messages.push({
        type: 'ai',
        content: finalText,
        _html: finalHTML
      })
      this._streamText = ''
      this.$nextTick(() => this.scrollToBottom())
    },

    /** 用户按停止 */
    _abortStream() {
      if (this._streamText && this.streaming) {
        this._onStreamDone()
      } else {
        this.streaming = false
      }
    },

    // === 发送消息 ===

    async sendMessage() {
      const msg = this.inputMessage.trim()
      if (!msg || this.loading) return

      this.messages.push({ type: 'user', content: msg, _html: `<p>${plainTextToHTML(msg)}</p>` })
      this.inputMessage = ''
      this.loading = true
      this._streamText = ''

      this.$nextTick(() => this.scrollToBottom())
      this.streaming = true

      // 等 Vue v-if 完成（DOM 出现）再开始读流
      await this.$nextTick()

      try {
        if (this.controller) this.controller.abort()
        this.controller = new AbortController()

        const response = await fetch('/api/agent/chat', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ memoryId: this.memoryId, message: msg }),
          signal: this.controller.signal
        })
        if (!response.ok) throw new Error(`HTTP ${response.status}`)

        await parseStream(response.body, {
          onToken: (t) => this._onStreamToken(t),
          onDone:  () => {},
          onError:  () => {}
        })
      } catch (err) {
        if (err.name !== 'AbortError') {
          console.error('Chat error:', err)
          this.$message.error('发送失败')
        }
      } finally {
        this.loading = false
        this.controller = null
        // flush 最后一批
        if (this._rafId) { cancelAnimationFrame(this._rafId); this._rafId = null }
        this._updateStreamDOM()
        // 只有还没被 abort 处理过时才 push 消息
        if (this.streaming) this._onStreamDone()
        this.$nextTick(() => this.scrollToBottom())
      }
    },

    stopStreaming() {
      if (this.controller) {
        this.controller.abort()
        this.loading = false
        this._abortStream()
      }
    },

    // === 滚动 ===

    scrollToBottom() {
      this.$nextTick(() => {
        const c = this.$refs.chatContainer
        if (c) c.scrollTop = c.scrollHeight
      })
    },
    scrollToBottomSmooth() {
      const c = this.$refs.chatContainer
      if (!c) return
      if (c.scrollHeight - c.scrollTop <= c.clientHeight + 200) {
        c.scrollTop = c.scrollHeight
      }
    }
  }
}

// ============ 纯函数 ============

/** 流式期间的轻量渲染：\n → <br> + HTML 转义 */
function plainTextToHTML(text) {
  if (!text) return ''
  return escapeHTML(text).replace(/\n/g, '<br>')
}

function escapeHTML(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

/** 流结束后全量 Markdown 解析 */
function safeFullParse(text) {
  try {
    return marked.parse(text, { breaks: true, gfm: true })
  } catch {
    try {
      return text.split(/\n\n+/)
        .map(p => { try { return marked.parse(p, { breaks: true, gfm: true }) } catch { return plainTextToHTML(p) } })
        .join('')
    } catch {
      return plainTextToHTML(text)
    }
  }
}
</script>

<!-- style unchanged from original -->
<style lang="scss" scoped>
.app-container {
  height: calc(100vh - 110px);
  padding: 10px;
  .card-container {
    background-color: #f8f0e8;
    border-radius: 10px;
    overflow: hidden; height: 100%;
    display: flex; flex-direction: column;
    .card-header {
      background: var(--indigo-blue); color: white;
      padding: 8px 15px; font-size: 16px; font-weight: bold;
      &::before { content: ""; display: inline-block; width: 4px; height: 16px; background: white; margin-right: 8px; }
    }
    .card-body { flex: 1; display: flex; flex-direction: column; padding: 10px; overflow: hidden; }
  }
}
.chat-container {
  flex: 1; overflow-y: auto; margin-bottom: 10px;
  background: #fff; border-radius: 8px; padding: 10px; border: 1px solid #ebeef5;
  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-track { background: rgba(0,0,0,.05); border-radius: 3px; }
  &::-webkit-scrollbar-thumb { background: var(--indigo-blue); opacity: .7; border-radius: 3px; }
}
.chat-messages { display: flex; flex-direction: column; }
.message {
  margin-bottom: 12px; max-width: 85%;
  &.user-message {
    align-self: flex-end;
    .message-content { background: #e3f2fd; border-radius: 15px 15px 0 15px; color: #333; }
  }
  &.ai-message {
    align-self: flex-start;
    .message-content { background: #f5f5f5; border-radius: 15px 15px 15px 0; color: #333; }
  }
  &.system-message {
    align-self: center; max-width: 95%; margin-bottom: 15px;
    .message-content { background: #f9f0e6; border-radius: 10px; color: #8b6f4e; border: 1px dashed #d4b894; font-size: 13px; }
  }
}
.message-content {
  padding: 10px 14px; line-height: 1.6; word-break: break-word;
  &.markdown-body {
    h1,h2,h3,h4,h5,h6 { margin: 10px 0 6px; font-weight: 600; line-height: 1.3; }
    h2 { font-size: 1.2em; border-bottom: 1px solid #e0e0e0; padding-bottom: 4px; }
    h3 { font-size: 1.1em; }
    p { margin: 4px 0 8px; }
    ul,ol { margin: 4px 0 8px; padding-left: 20px; }
    li { margin-bottom: 2px; }
    strong { font-weight: 600; color: #222; }
    code { background: #f0f0f0; padding: 1px 4px; border-radius: 3px; font-size: .9em; }
    pre { background: #f5f5f5; padding: 8px 12px; border-radius: 5px; overflow-x: auto; margin: 8px 0; code { background: none; padding: 0; } }
    blockquote { border-left: 3px solid #d0d0d0; padding-left: 10px; margin: 8px 0; color: #666; }
    table { border-collapse: collapse; margin: 8px 0; th,td { border: 1px solid #ddd; padding: 4px 8px; font-size: .9em; } th { background: #f5f5f5; } }
    hr { border: none; border-top: 1px solid #e0e0e0; margin: 12px 0; }
  }
}
.loading-indicator {
  display: flex; align-items: center; justify-content: center;
  .dot {
    display: inline-block; width: 8px; height: 8px; border-radius: 50%;
    background: #999; margin: 0 3px; animation: dot-flashing 1s infinite alternate;
    &:nth-child(2) { animation-delay: .2s; }
    &:nth-child(3) { animation-delay: .4s; }
  }
}
@keyframes dot-flashing { 0% { opacity: .2; } 100% { opacity: 1; } }
.input-container {
  display: flex; flex-direction: column; background: #fff; border-radius: 8px; padding: 10px; border: 1px solid #ebeef5;
}
.button-container { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.shortcut-tip { font-size: 12px; color: #909399; }
::v-deep .el-textarea__inner { resize: none; border-color: #d4b894; min-height: 60px !important; &:focus { border-color: var(--indigo-blue); } }
</style>
