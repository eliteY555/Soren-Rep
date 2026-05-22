<template>
  <div :class="['message-bubble', role === 'USER' ? 'user' : 'assistant']">
    <div class="bubble-header">
      <span class="role-label">{{ role === 'USER' ? '你' : 'AI' }}</span>
      <span v-if="providerName" class="provider-tag">{{ providerName }}</span>
      <span v-if="mode === 'RAG'" class="mode-tag">知识库</span>
    </div>
    <div class="bubble-content" v-html="renderedContent"></div>
    <span v-if="streaming" class="cursor-blink">|</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { marked } from 'marked'

const props = defineProps({
  role: String, content: String, providerName: String,
  mode: String, streaming: Boolean
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
.bubble-content :deep(code) { font-family: 'Fira Code', monospace; font-size: 13px; }
.cursor-blink { animation: blink 1s step-end infinite; }
@keyframes blink { 50% { opacity: 0; } }
</style>
