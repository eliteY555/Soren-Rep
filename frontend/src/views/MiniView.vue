<template>
  <div class="mini-root">
    <!-- 标题栏 -->
    <header class="mini-bar">
      <span class="mini-bar-brand">◈ AI 助手</span>
      <span v-if="configStore.activeProviderName" class="mini-bar-chip">{{ configStore.activeProviderName }}</span>
      <button
        class="mini-mode-btn"
        :class="{ active: configStore.chatMode === 'RAG' }"
        @click="toggleMode"
        title="切换知识库增强模式"
      >
        {{ configStore.chatMode === 'RAG' ? '知识库' : '直接' }}
      </button>
      <button class="mini-back-btn" @click="goBack" title="返回主窗口">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 3h6v6"/><path d="M10 14L21 3"/><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
        </svg>
        <span>返回</span>
      </button>
    </header>

    <!-- 消息区域 -->
    <div class="mini-chat">
      <div v-if="chatStore.messages.length === 0" class="mini-empty">
        <p class="mini-empty-title">AI 助手</p>
        <p class="mini-empty-desc">输入问题开始对话</p>
      </div>
      <div class="mini-messages" v-else>
        <div v-for="(msg, idx) in chatStore.messages" :key="idx" class="mini-msg">
          <div v-if="msg.streaming && !msg.content" class="mini-loading">
            <span class="lds-dot" /><span class="lds-dot" /><span class="lds-dot" />
          </div>
          <div v-else :class="['mini-bubble', msg.role === 'USER' ? 'user' : 'ai']" v-html="renderContent(msg.content)" />
        </div>
        <div ref="bottomRef" />
      </div>
    </div>

    <!-- 输入区域 -->
    <footer class="mini-input">
      <div class="mini-input-row">
        <VoiceButton
          :isSupported="speech.isSupported.value"
          :isListening="speech.isListening.value"
          :isPTT="speech.mode.value === 'ptt'"
          @start="speech.startListening()"
          @stop="speech.stopListening()"
        />
        <input
          v-model="textInput"
          class="mini-text-input"
          placeholder="输入问题…"
          @keydown.enter="sendText"
          :disabled="chatStore.isStreaming"
        />
        <button
          class="mini-send-btn"
          @click="sendText"
          :disabled="!textInput.trim() || chatStore.isStreaming"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
          </svg>
        </button>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { useChatStore } from '../stores/chatStore'
import { useConfigStore } from '../stores/configStore'
import { useSpeech } from '../composables/useSpeech'
import { safeMarkdown } from '../marked-setup'
import VoiceButton from '../components/VoiceButton.vue'

const chatStore = useChatStore()
const configStore = useConfigStore()
const speech = useSpeech()

const textInput = ref('')
const bottomRef = ref(null)

onMounted(async () => {
  await configStore.loadProviders()
  configStore.restoreMode()
  await chatStore.loadSessions()

  const savedId = sessionStorage.getItem('ai-active-session')
  if (savedId) {
    await chatStore.switchSession(savedId)
  }
})

// Scroll to bottom
watch(() => chatStore.messages.length, () => {
  nextTick(() => bottomRef.value?.scrollIntoView({ behavior: 'smooth' }))
})
watch(() => {
  const msgs = chatStore.messages
  return msgs && msgs.length ? msgs[msgs.length - 1].content : ''
}, () => {
  nextTick(() => bottomRef.value?.scrollIntoView({ behavior: 'smooth' }))
})

watch(() => speech.transcript.value, (val) => {
  if (val) textInput.value = val
})

function renderContent(content) {
  if (!content) return ''
  return safeMarkdown(content)
}

function toggleMode() {
  const next = configStore.chatMode === 'RAG' ? 'DIRECT' : 'RAG'
  configStore.setChatMode(next)
}

function sendText() {
  const content = textInput.value.trim()
  if (!content) return
  textInput.value = ''
  chatStore.sendMessage(content, configStore.chatMode, configStore.activeProviderId)
}

function goBack() {
  // 关闭弹窗，聚焦主窗口
  if (window.opener) {
    window.opener.focus()
  }
  window.close()
}
</script>

<style>
/* Mini window — standalone page, no scoped */
html, body, #app { margin: 0; padding: 0; height: 100%; overflow: hidden; }

:root {
  --bg-primary: #0B1120;
  --bg-secondary: #111827;
  --bg-card: #1A2332;
  --bg-hover: #1E293B;
  --bg-input: #151D2B;
  --text-primary: #F1F5F9;
  --text-secondary: #94A3B8;
  --text-muted: #64748B;
  --accent: #22C55E;
  --accent-hover: #16A34A;
  --accent-soft: rgba(34, 197, 94, 0.12);
  --user-bubble: #22C55E;
  --user-bubble-text: #FFFFFF;
  --ai-bubble: #1E293B;
  --ai-bubble-text: #E2E8F0;
  --border: #1E293B;
  --border-light: #263348;
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --font-mono: 'Fira Code', 'Cascadia Code', 'JetBrains Mono', monospace;
}
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: var(--font-sans);
  background: var(--bg-primary);
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
  line-height: 1.6;
}
::-webkit-scrollbar { width: 5px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--border-light); border-radius: 3px; }
</style>

<style scoped>
.mini-root { display: flex; flex-direction: column; height: 100vh; background: var(--bg-primary); }

/* Title bar */
.mini-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 0 12px; height: 34px; flex-shrink: 0;
  background: var(--bg-secondary); border-bottom: 1px solid var(--border);
}
.mini-bar-brand { font-size: 12px; font-weight: 700; color: var(--text-secondary); }
.mini-bar-chip {
  font-size: 10px; padding: 1px 6px; border-radius: 8px;
  background: var(--bg-hover); color: var(--text-muted);
}
.mini-mode-btn {
  font-size: 10px; padding: 2px 8px; border-radius: 8px;
  background: var(--bg-hover); border: 1px solid transparent;
  color: var(--text-muted); cursor: pointer; font-family: inherit;
  transition: all 0.15s ease;
}
.mini-mode-btn:hover { color: var(--text-primary); }
.mini-mode-btn.active {
  background: rgba(34,197,94,0.15); color: var(--accent);
  border-color: rgba(34,197,94,0.3);
}
.mini-back-btn {
  display: flex; align-items: center; gap: 4px; margin-left: auto;
  padding: 3px 8px; background: transparent; border: 1px solid var(--border);
  border-radius: 4px; color: var(--text-muted); cursor: pointer;
  font-size: 11px; font-family: inherit; transition: all 0.15s ease;
}
.mini-back-btn:hover { background: var(--bg-hover); color: var(--accent); border-color: var(--accent); }

/* Chat */
.mini-chat { flex: 1; overflow-y: auto; padding: 10px 12px; }
.mini-empty { text-align: center; padding-top: 35%; opacity: 0.5; }
.mini-empty-title { font-size: 14px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.mini-empty-desc { font-size: 12px; color: var(--text-muted); }

/* Loading dots */
.mini-loading {
  display: flex; align-items: center; gap: 4px;
  padding: 10px 14px; margin-right: auto;
}
.lds-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: var(--accent); opacity: 0.4;
  animation: dotPulseMini 1.4s ease-in-out infinite;
}
.lds-dot:nth-child(2) { animation-delay: 0.2s; }
.lds-dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes dotPulseMini {
  0%, 80%, 100% { opacity: 0.2; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1.2); }
}
.mini-msg { margin-bottom: 12px; }
.mini-bubble {
  max-width: 94%; padding: 8px 12px; border-radius: 10px;
  font-size: 13px; line-height: 1.6; word-break: break-word;
}
.mini-bubble.user {
  background: var(--user-bubble); color: var(--user-bubble-text);
  margin-left: auto; border-bottom-right-radius: 3px;
}
.mini-bubble.ai {
  background: var(--ai-bubble); color: var(--ai-bubble-text);
  margin-right: auto; border-bottom-left-radius: 3px;
  border: 1px solid var(--border);
}

/* Markdown */
.mini-bubble :deep(h1) { font-size: 1.15em; margin: 10px 0 6px; }
.mini-bubble :deep(h2) { font-size: 1.08em; margin: 8px 0 4px; }
.mini-bubble :deep(h3) { font-size: 1.02em; margin: 6px 0 3px; }
.mini-bubble :deep(p) { margin: 0 0 6px 0; }
.mini-bubble :deep(p:last-child) { margin-bottom: 0; }
.mini-bubble :deep(ul), .mini-bubble :deep(ol) { padding-left: 16px; margin: 4px 0; }
.mini-bubble :deep(li) { margin-bottom: 2px; }
.mini-bubble :deep(code) {
  font-size: 0.85em; background: rgba(255,255,255,0.08); color: #e879f9;
  padding: 1px 4px; border-radius: 3px;
}
.mini-bubble :deep(pre) {
  margin: 6px 0; padding: 8px 10px; border-radius: 6px;
  background: #0D1520; border: 1px solid var(--border);
  font-size: 11.5px; overflow-x: auto;
}
.mini-bubble :deep(pre code) { background: transparent; padding: 0; }
.mini-bubble :deep(blockquote) {
  border-left: 2px solid var(--accent); margin: 6px 0; padding: 4px 8px;
  background: rgba(34,197,94,0.05); border-radius: 0 6px 6px 0;
  color: var(--text-secondary); font-size: 12px;
}
.mini-bubble :deep(a) { color: var(--accent); }

/* Input */
.mini-input { padding: 8px 10px; flex-shrink: 0; border-top: 1px solid var(--border); background: var(--bg-secondary); }
.mini-input-row { display: flex; align-items: center; gap: 8px; }
.mini-text-input {
  flex: 1; padding: 7px 10px; background: var(--bg-input);
  border: 1px solid var(--border); border-radius: 6px;
  color: var(--text-primary); font-size: 12.5px; font-family: inherit;
  outline: none; transition: border-color 0.2s ease;
}
.mini-text-input:focus { border-color: var(--accent); }
.mini-text-input::placeholder { color: var(--text-muted); font-size: 12px; }
.mini-text-input:disabled { opacity: 0.5; }
.mini-send-btn {
  width: 32px; height: 32px; flex-shrink: 0; display: flex; align-items: center; justify-content: center;
  border-radius: 6px; border: none; cursor: pointer; transition: all 0.2s ease;
  background: var(--bg-input); color: var(--text-muted);
}
.mini-send-btn:not(:disabled) { background: var(--accent); color: #fff; }
.mini-send-btn:not(:disabled):hover { background: var(--accent-hover); }
.mini-send-btn:disabled { cursor: not-allowed; opacity: 0.4; }
</style>
