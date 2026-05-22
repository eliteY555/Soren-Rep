<template>
  <div :class="['msg-row', role === 'USER' ? 'user' : 'ai']">
    <div class="msg-avatar" v-if="role !== 'USER'">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
      </svg>
    </div>
    <div :class="['msg-body', role === 'USER' ? 'user-body' : 'ai-body']">
      <div class="msg-meta">
        <span class="msg-role">{{ role === 'USER' ? '你' : 'AI 助手' }}</span>
        <span v-if="mode === 'RAG'" class="msg-mode">知识库</span>
        <span class="msg-time">{{ formatTime(timestamp) }}</span>
      </div>
      <div class="msg-content" v-html="renderedContent" />
      <span v-if="streaming" class="typing-cursor" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { marked } from 'marked'

const props = defineProps({
  role: String, content: String, providerName: String,
  mode: String, streaming: Boolean, timestamp: String
})

const renderedContent = computed(() => {
  if (!props.content) return '<span class="streaming-placeholder">思考中…</span>'
  return marked(props.content, { breaks: true })
})

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return d.getHours().toString().padStart(2,'0') + ':' + d.getMinutes().toString().padStart(2,'0')
}
</script>

<style scoped>
.msg-row {
  display: flex; gap: 12px; margin-bottom: 20px;
  align-items: flex-start;
}
.msg-row.user { justify-content: flex-end; }
.msg-row.ai { justify-content: flex-start; }

.msg-avatar {
  width: 34px; height: 34px; flex-shrink: 0;
  border-radius: var(--radius-sm); background: var(--bg-card);
  display: flex; align-items: center; justify-content: center;
  color: var(--accent); border: 1px solid var(--border);
}

.msg-body { max-width: 72%; }
.user-body .msg-meta { justify-content: flex-end; }

.msg-meta {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 6px; padding: 0 4px;
  font-size: 12px; color: var(--text-muted);
}
.msg-role { font-weight: 600; color: var(--text-secondary); }
.msg-mode {
  font-size: 11px; padding: 1px 7px; border-radius: 10px;
  background: rgba(34,197,94,0.15); color: var(--accent);
  font-weight: 500;
}
.msg-time { font-variant-numeric: tabular-nums; }

.msg-content {
  padding: 14px 18px; border-radius: var(--radius-lg);
  font-size: 14.5px; line-height: 1.68; word-break: break-word;
}
.user-body .msg-content {
  background: var(--user-bubble); color: var(--user-bubble-text);
  border-bottom-right-radius: 6px;
}
.ai-body .msg-content {
  background: var(--ai-bubble); color: var(--ai-bubble-text);
  border: 1px solid var(--border); border-bottom-left-radius: 6px;
}

.msg-content :deep(p) { margin: 0 0 10px 0; }
.msg-content :deep(p:last-child) { margin-bottom: 0; }
.msg-content :deep(ul), .msg-content :deep(ol) { margin: 8px 0; padding-left: 20px; }
.msg-content :deep(li) { margin-bottom: 4px; }
.msg-content :deep(strong) { font-weight: 600; }
.msg-content :deep(pre) {
  background: #0D1520; padding: 14px 16px; border-radius: var(--radius-sm);
  overflow-x: auto; margin: 10px 0; font-size: 13px;
  border: 1px solid var(--border);
}
.msg-content :deep(code) {
  font-family: var(--font-mono); font-size: 13px;
  background: rgba(255,255,255,0.06); padding: 2px 6px; border-radius: 4px;
}
.msg-content :deep(pre code) {
  background: none; padding: 0; border-radius: 0;
}
.msg-content :deep(blockquote) {
  border-left: 3px solid var(--accent); padding-left: 14px;
  margin: 10px 0; color: var(--text-secondary);
}
.msg-content :deep(table) {
  width: 100%; border-collapse: collapse; margin: 10px 0;
  font-size: 13px;
}
.msg-content :deep(th), .msg-content :deep(td) {
  padding: 8px 12px; border: 1px solid var(--border); text-align: left;
}
.msg-content :deep(th) { background: var(--bg-hover); font-weight: 600; }

.streaming-placeholder { color: var(--text-muted); font-style: italic; }

.typing-cursor {
  display: inline-block; width: 2px; height: 18px;
  background: var(--accent); margin-left: 2px; vertical-align: text-bottom;
  animation: blink 0.8s step-end infinite;
}
@keyframes blink { 50% { opacity: 0; } }
</style>
