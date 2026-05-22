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
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

const props = defineProps({
  role: String, content: String, providerName: String,
  mode: String, streaming: Boolean, timestamp: String
})

const contentRef = ref(null)

// Configure marked with highlight.js for code blocks
marked.setOptions({
  breaks: true,
  gfm: true,
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (_) {}
    }
    try {
      return hljs.highlightAuto(code).value
    } catch (_) {}
    return code
  }
})

// Custom renderer for cleaner output
const renderer = new marked.Renderer()

// Headings with anchor-like style
renderer.heading = function({ text, depth }) {
  const sizes = { 1: '1.5em', 2: '1.25em', 3: '1.1em', 4: '1em' }
  const margins = { 1: '24px 0 16px', 2: '20px 0 12px', 3: '16px 0 10px', 4: '14px 0 8px' }
  const borders = depth <= 2
  return `<h${depth} class="md-heading" style="font-size:${sizes[depth]}; margin:${margins[depth]}; ${borders ? 'border-bottom:1px solid var(--border); padding-bottom:8px;' : ''}">${text}</h${depth}>`
}

// Cleaner code blocks with language label
renderer.code = function({ text, lang }) {
  const langLabel = lang ? `<span class="code-lang">${lang}</span>` : ''
  const copyBtn = `<button class="code-copy" onclick="navigator.clipboard.writeText(this.dataset.code)" data-code="${escapeHtml(text)}">
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
  </button>`
  const highlighted = hljs.getLanguage(lang)
    ? hljs.highlight(text, { language: lang }).value
    : hljs.highlightAuto(text).value
  return `<div class="code-block">${langLabel}${copyBtn}<pre><code class="hljs">${highlighted}</code></pre></div>`
}

// Better paragraph spacing
renderer.paragraph = function({ text }) {
  return `<p class="md-p">${text}</p>`
}

// Better list styling
renderer.list = function({ items, ordered }) {
  const tag = ordered ? 'ol' : 'ul'
  const cls = ordered ? 'md-ol' : 'md-ul'
  const itemsHtml = items.map(item => `<li class="md-li">${item.text}</li>`).join('')
  return `<${tag} class="${cls}">${itemsHtml}</${tag}>`
}

// Blockquote
renderer.blockquote = function({ text }) {
  return `<blockquote class="md-quote">${text}</blockquote>`
}

// Horizontal rule
renderer.hr = function() {
  return `<hr class="md-hr" />`
}

// Table
renderer.table = function({ header, rows }) {
  const headerHtml = `<thead><tr>${header.map(h => `<th>${h.text}</th>`).join('')}</tr></thead>`
  const bodyHtml = `<tbody>${rows.map(row => `<tr>${row.map(cell => `<td>${cell.text}</td>`).join('')}</tr>`).join('')}</tbody>`
  return `<div class="md-table-wrap"><table class="md-table">${headerHtml}${bodyHtml}</table></div>`
}

// Image
renderer.image = function({ href, title, text }) {
  return `<figure class="md-figure"><img src="${href}" alt="${text}" title="${title || ''}" loading="lazy" /><figcaption>${text}</figcaption></figure>`
}

marked.use({ renderer })

// Streaming optimization: throttle re-renders
let renderTimer = null
const renderedContent = computed(() => {
  if (!props.content) return '<span class="streaming-placeholder">思考中…</span>'
  // During streaming, marked parses partial markdown fine
  return marked.parse(props.content)
})

// Add copy buttons hook after content renders
watch(() => props.content, () => {
  nextTick(() => attachCopyHandlers())
})

onMounted(() => {
  nextTick(() => attachCopyHandlers())
})

function attachCopyHandlers() {
  if (!contentRef.value) return
  contentRef.value.querySelectorAll('.code-copy').forEach(btn => {
    if (btn._bound) return
    btn._bound = true
    btn.addEventListener('click', () => {
      const code = btn.dataset.code
      navigator.clipboard.writeText(code).then(() => {
        btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>'
        setTimeout(() => {
          btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>'
        }, 2000)
      })
    })
  })
}

function escapeHtml(text) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return d.getHours().toString().padStart(2,'0') + ':' + d.getMinutes().toString().padStart(2,'0')
}
</script>

<style scoped>
/* --- Layout --- */
.msg-row {
  display: flex; gap: 12px; margin-bottom: 24px;
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
  background: rgba(34,197,94,0.15); color: var(--accent);
  font-weight: 500;
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
.msg-content :deep(.md-heading) {
  font-weight: 700; letter-spacing: -0.2px;
  color: var(--text-primary);
}
.msg-content :deep(h1.md-heading) { font-size: 1.4em; }
.msg-content :deep(h2.md-heading) { font-size: 1.2em; }
.msg-content :deep(h3.md-heading) { font-size: 1.08em; }
.msg-content :deep(h4.md-heading) { font-size: 1em; }

/* --- Paragraph --- */
.msg-content :deep(.md-p) { margin: 0 0 12px 0; }
.msg-content :deep(.md-p:last-child) { margin-bottom: 0; }

/* --- Lists --- */
.msg-content :deep(.md-ul), .msg-content :deep(.md-ol) {
  margin: 10px 0; padding-left: 22px;
}
.msg-content :deep(.md-li) {
  margin-bottom: 6px; padding-left: 2px;
}
.msg-content :deep(.md-li::marker) {
  color: var(--text-muted);
}

/* --- Inline code --- */
.msg-content :deep(code:not(pre code)) {
  font-family: var(--font-mono); font-size: 0.9em;
  background: rgba(255,255,255,0.08); color: #e879f9;
  padding: 2px 7px; border-radius: 4px;
  white-space: nowrap;
}

/* --- Code block --- */
.msg-content :deep(.code-block) {
  position: relative; margin: 14px 0; border-radius: var(--radius-sm);
  border: 1px solid var(--border); overflow: hidden;
  background: #0D1520;
}
.msg-content :deep(.code-lang) {
  position: absolute; top: 8px; left: 14px;
  font-size: 11px; color: var(--text-muted); font-family: var(--font-mono);
  text-transform: uppercase; letter-spacing: 0.5px;
}
.msg-content :deep(.code-copy) {
  position: absolute; top: 6px; right: 8px;
  background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-muted); border-radius: 5px; cursor: pointer;
  padding: 5px 7px; display: flex; align-items: center;
  transition: all 0.15s ease; z-index: 1;
}
.msg-content :deep(.code-copy:hover) {
  background: rgba(255,255,255,0.12); color: var(--text-primary);
}
.msg-content :deep(.code-block pre) {
  margin: 0; padding: 38px 16px 14px; overflow-x: auto;
  font-size: 13.5px; line-height: 1.6;
  background: transparent;
}
.msg-content :deep(.code-block pre code) {
  font-family: var(--font-mono); background: transparent; padding: 0;
}

/* --- Blockquote --- */
.msg-content :deep(.md-quote) {
  border-left: 3px solid var(--accent);
  margin: 14px 0; padding: 10px 16px;
  background: rgba(34,197,94,0.05); border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  color: var(--text-secondary); font-style: italic;
}

/* --- Horizontal rule --- */
.msg-content :deep(.md-hr) {
  border: none; border-top: 1px solid var(--border);
  margin: 20px 0;
}

/* --- Table --- */
.msg-content :deep(.md-table-wrap) {
  margin: 14px 0; overflow-x: auto; border-radius: var(--radius-sm);
  border: 1px solid var(--border);
}
.msg-content :deep(.md-table) {
  width: 100%; border-collapse: collapse; font-size: 13.5px;
}
.msg-content :deep(.md-table th) {
  background: rgba(255,255,255,0.04); font-weight: 600;
  padding: 10px 14px; text-align: left; white-space: nowrap;
  border-bottom: 1px solid var(--border); color: var(--text-primary);
}
.msg-content :deep(.md-table td) {
  padding: 9px 14px; border-bottom: 1px solid var(--border);
  color: var(--text-secondary);
}
.msg-content :deep(.md-table tr:last-child td) { border-bottom: none; }
.msg-content :deep(.md-table tr:hover td) { background: rgba(255,255,255,0.02); }

/* --- Images --- */
.msg-content :deep(.md-figure) {
  margin: 14px 0; text-align: center;
}
.msg-content :deep(.md-figure img) {
  max-width: 100%; border-radius: var(--radius-sm);
}
.msg-content :deep(.md-figure figcaption) {
  font-size: 12px; color: var(--text-muted); margin-top: 6px;
}

/* --- Bold / Italic --- */
.msg-content :deep(strong) { font-weight: 700; color: var(--text-primary); }

/* --- Links --- */
.msg-content :deep(a) {
  color: var(--accent); text-decoration: underline;
  text-underline-offset: 2px;
}
.msg-content :deep(a:hover) { color: var(--accent-hover); }

/* --- Streaming --- */
.streaming-placeholder { color: var(--text-muted); font-style: italic; }
.typing-cursor {
  display: inline-block; width: 2px; height: 18px;
  background: var(--accent); margin-left: 2px; vertical-align: text-bottom;
  animation: blink 0.8s step-end infinite;
  border-radius: 1px;
}
@keyframes blink { 50% { opacity: 0; } }
</style>
