<template>
  <div class="chat-panel" ref="panelRef">
    <div v-if="messages.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
          <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
          <line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/>
        </svg>
      </div>
      <h2 class="empty-title">语音 AI 助手</h2>
      <p class="empty-desc">按下麦克风按钮开始语音提问，或直接输入文字</p>
      <div class="empty-hints">
        <span>💡 试试知识库增强模式，上传文档后提问更精准</span>
        <span>🔄 支持 DeepSeek / 通义千问 / OpenAI 多模型切换</span>
      </div>
    </div>
    <div class="messages-container">
      <MessageBubble v-for="(msg, idx) in messages" :key="idx" v-bind="msg" />
    </div>
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

// Also scroll when streaming content changes
watch(() => {
  const msgs = props.messages
  return msgs && msgs.length ? msgs[msgs.length - 1].content : ''
}, () => {
  nextTick(() => bottomRef.value?.scrollIntoView({ behavior: 'smooth' }))
})
</script>

<style scoped>
.chat-panel {
  flex: 1; overflow-y: auto;
  background: var(--bg-primary);
}

.messages-container {
  max-width: 800px; margin: 0 auto;
  padding: 24px 24px 8px;
}

.empty-state {
  text-align: center; padding-top: 18vh;
  max-width: 460px; margin: 0 auto;
}
.empty-icon { color: var(--text-muted); margin-bottom: 20px; opacity: 0.5; }
.empty-title { font-size: 22px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; letter-spacing: -0.3px; }
.empty-desc { font-size: 15px; color: var(--text-muted); margin-bottom: 24px; }
.empty-hints {
  display: flex; flex-direction: column; gap: 10px;
  font-size: 13px; color: var(--text-muted);
  padding: 16px; background: var(--bg-secondary);
  border-radius: var(--radius-md); border: 1px solid var(--border);
}
</style>
