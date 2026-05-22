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
        <span v-if="mode === 'RAG'" class="msg-mode">知识库增强</span>
        <span class="msg-time">{{ formatTime(timestamp) }}</span>
      </div>
      <div class="msg-content" ref="contentRef" v-html="renderedContent" />
      <span v-if="streaming" class="typing-cursor" />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch, nextTick, onMounted } from 'vue'
import { safeMarkdown, safeStreamingMarkdown } from '../marked-setup.js'

const props = defineProps({
  role: String, content: String, providerName: String,
  mode: String, streaming: Boolean, timestamp: String
})

const contentRef = ref(null)

const renderedContent = computed(() => {
  if (!props.content) return '<span class="streaming-placeholder">思考中…</span>'
  // During streaming: only render complete paragraphs as markdown,
  // the in-progress paragraph stays as plain text to avoid half-rendered syntax
  return props.streaming ? safeStreamingMarkdown(props.content) : safeMarkdown(props.content)
})

// Copy button DOM handlers
watch(() => props.content, () => nextTick(attachCopyHandlers))
onMounted(() => nextTick(attachCopyHandlers))

function attachCopyHandlers() {
  if (!contentRef.value) return
  contentRef.value.querySelectorAll('.code-copy').forEach(btn => {
    if (btn._bound) return
    btn._bound = true
    btn.addEventListener('click', () => {
      const code = btn.dataset.code
      navigator.clipboard.writeText(code || '').then(() => {
        btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>'
        setTimeout(() => {
          btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>'
        }, 2000)
      })
    })
  })
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return d.getHours().toString().padStart(2,'0') + ':' + d.getMinutes().toString().padStart(2,'0')
}
</script>

<style scoped>
/* --- Layout --- */
.msg-row { display: flex; gap: 12px; margin-bottom: 24px; align-items: flex-start; }
.msg-row.user { justify-content: flex-end; }
.msg-row.ai { justify-content: flex-start; }

.msg-avatar {
  width: 34px; height: 34px; flex-shrink: 0;
  border-radius: var(--radius-sm); background: var(--bg-card);
  display: flex; align-items: center; justify-content: center;
  color: var(--accent); border: 1px solid var(--border);
}

.msg-body { max-width: 74%; min-width: 0; }
.user-body .msg-meta { justify-content: flex-end; }

.msg-meta {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 6px; padding: 0 4px;
  font-size: 12px; color: var(--text-muted);
}
.msg-role { font-weight: 600; color: var(--text-secondary); }
.msg-mode {
  font-size: 11px; padding: 2px 8px; border-radius: 10px;
  background: rgba(34,197,94,0.15); color: var(--accent); font-weight: 500;
}
.msg-time { font-variant-numeric: tabular-nums; }

/* --- Bubble container --- */
.msg-content {
  padding: 18px 20px; border-radius: var(--radius-lg);
  font-size: 15px; line-height: 1.75; word-break: break-word;
  color: var(--ai-bubble-text);
}
.user-body .msg-content {
  background: var(--user-bubble); color: var(--user-bubble-text);
  border-bottom-right-radius: 6px;
}
.ai-body .msg-content {
  background: var(--ai-bubble);
  border: 1px solid var(--border); border-bottom-left-radius: 6px;
}

/* --- Headings --- */
.msg-content :deep(h1) { font-size: 1.4em; font-weight: 700; margin: 24px 0 16px; border-bottom: 1px solid var(--border); padding-bottom: 8px; }
.msg-content :deep(h2) { font-size: 1.2em; font-weight: 700; margin: 20px 0 12px; border-bottom: 1px solid var(--border); padding-bottom: 6px; }
.msg-content :deep(h3) { font-size: 1.08em; font-weight: 700; margin: 16px 0 10px; }
.msg-content :deep(h4) { font-size: 1em; font-weight: 700; margin: 14px 0 8px; }

/* --- Paragraph --- */
.msg-content :deep(p) { margin: 0 0 12px 0; }
.msg-content :deep(p:last-child) { margin-bottom: 0; }

/* --- Lists --- */
.msg-content :deep(ul), .msg-content :deep(ol) { margin: 10px 0; padding-left: 22px; }
.msg-content :deep(li) { margin-bottom: 6px; }
.msg-content :deep(li::marker) { color: var(--text-muted); }

/* --- Inline code --- */
.msg-content :deep(code) {
  font-family: var(--font-mono); font-size: 0.9em;
  background: rgba(255,255,255,0.08); color: #e879f9;
  padding: 2px 7px; border-radius: 4px;
}

/* --- Code block --- */
.msg-content :deep(.code-block) {
  position: relative; margin: 14px 0; border-radius: var(--radius-sm);
  border: 1px solid var(--border); overflow: hidden; background: #0D1520;
}
.msg-content :deep(.code-lang) {
  position: absolute; top: 8px; left: 14px;
  font-size: 11px; color: var(--text-muted); font-family: var(--font-mono);
  text-transform: uppercase; letter-spacing: 0.5px;
}
.msg-content :deep(.code-copy) {
  position: absolute; top: 6px; right: 8px; z-index: 1;
  background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-muted); border-radius: 5px; cursor: pointer;
  padding: 5px 7px; display: flex; align-items: center;
  transition: all 0.15s ease;
}
.msg-content :deep(.code-copy:hover) { background: rgba(255,255,255,0.12); color: var(--text-primary); }
.msg-content :deep(.code-block pre) {
  margin: 0; padding: 38px 16px 14px; overflow-x: auto;
  font-size: 13.5px; line-height: 1.6; background: transparent;
}
.msg-content :deep(.code-block pre code) {
  font-family: var(--font-mono); background: transparent; padding: 0; color: inherit;
}

/* --- Blockquote --- */
.msg-content :deep(blockquote) {
  border-left: 3px solid var(--accent); margin: 14px 0; padding: 10px 16px;
  background: rgba(34,197,94,0.05); border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  color: var(--text-secondary);
}
.msg-content :deep(blockquote p) { margin: 0; }

/* --- Horizontal rule --- */
.msg-content :deep(hr) { border: none; border-top: 1px solid var(--border); margin: 20px 0; }

/* --- Table --- */
.msg-content :deep(table) {
  width: 100%; border-collapse: collapse; margin: 14px 0; font-size: 13.5px;
  border: 1px solid var(--border); border-radius: var(--radius-sm); overflow: hidden;
}
.msg-content :deep(th) {
  background: rgba(255,255,255,0.04); font-weight: 600;
  padding: 10px 14px; text-align: left; border-bottom: 2px solid var(--border);
}
.msg-content :deep(td) { padding: 9px 14px; border-bottom: 1px solid var(--border); }
.msg-content :deep(tr:last-child td) { border-bottom: none; }

/* --- Images --- */
.msg-content :deep(img) { max-width: 100%; border-radius: var(--radius-sm); margin: 10px 0; }

/* --- Bold / Italic / Links --- */
.msg-content :deep(strong) { font-weight: 700; color: var(--text-primary); }
.msg-content :deep(a) { color: var(--accent); text-underline-offset: 2px; }
.msg-content :deep(a:hover) { color: var(--accent-hover); }

/* --- Streaming --- */
.streaming-placeholder { color: var(--text-muted); font-style: italic; }
.typing-cursor {
  display: inline-block; width: 2px; height: 18px;
  background: var(--accent); margin-left: 2px; vertical-align: text-bottom;
  animation: blink 0.8s step-end infinite; border-radius: 1px;
}
@keyframes blink { 50% { opacity: 0; } }
</style>
