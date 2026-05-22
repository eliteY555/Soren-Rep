<template>
  <div class="home">
    <!-- ========== 完整模式 ========== -->
    <template v-if="!isMini">
      <aside class="sidebar">
        <div class="sidebar-brand">
          <span class="brand-icon">◈</span>
          <span class="brand-text">AI 助手</span>
        </div>
        <div class="sidebar-tabs">
          <button :class="['tab-btn', sidebarTab === 'chat' && 'active']" @click="sidebarTab = 'chat'">
            对话
          </button>
          <button :class="['tab-btn', sidebarTab === 'knowledge' && 'active']" @click="sidebarTab = 'knowledge'">
            知识库
          </button>
        </div>
        <div class="sidebar-content">
          <SessionList
            v-if="sidebarTab === 'chat'"
            :sessions="chatStore.sessions"
            :currentId="chatStore.currentSessionId"
            @newSession="chatStore.createNewSession()"
            @switch="chatStore.switchSession($event)"
            @delete="chatStore.deleteCurrentSession($event)"
          />
          <KnowledgePanel v-if="sidebarTab === 'knowledge'" />
        </div>
      </aside>

      <main class="main">
        <header class="topbar">
          <div class="topbar-left">
            <ProviderSelector
              :providers="configStore.providers"
              :activeId="configStore.activeProviderId"
              @change="configStore.activateProvider($event)"
            />
            <ModeSwitch :mode="configStore.chatMode" @change="configStore.setChatMode($event)" />
          </div>
          <div class="topbar-right">
            <el-button size="small" text @click="toggleSpeechMode" class="mode-toggle">
              {{ speech.mode.value === 'ptt' ? '实时模式' : '按键模式' }}
            </el-button>
            <el-button size="small" text :icon="Setting" @click="showProviderDialog = true" class="settings-btn" />
          </div>
        </header>

        <ChatPanel :messages="chatStore.messages" />

        <footer class="input-area">
          <div class="input-row">
            <VoiceButton
              :isSupported="speech.isSupported.value"
              :isListening="speech.isListening.value"
              :isPTT="speech.mode.value === 'ptt'"
              @start="speech.startListening()"
              @stop="speech.stopListening()"
            />
            <div class="text-input-wrap">
              <input
                v-model="textInput"
                class="text-input"
                placeholder="输入问题，或使用麦克风语音输入…"
                @keydown.enter="sendText"
                :disabled="chatStore.isStreaming"
                ref="inputRef"
              />
            </div>
            <button
              class="send-btn"
              @click="sendText"
              :disabled="!textInput.trim() || chatStore.isStreaming"
              :class="{ active: textInput.trim() && !chatStore.isStreaming }"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
              </svg>
            </button>
            <button class="mini-toggle-btn" @click="enterMiniMode" title="小窗模式">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="4 8 4 4 8 4"/><line x1="20" y1="4" x2="12" y2="12"/>
                <polyline points="4 20 4 16"/><polyline points="20 20 20 16"/>
              </svg>
            </button>
          </div>
          <div v-if="speech.transcript.value || speech.interimTranscript.value" class="transcript-preview">
            <span class="final-text">{{ speech.transcript.value }}</span>
            <span class="interim-text">{{ speech.interimTranscript.value }}</span>
            <button class="transcript-action" @click="sendTranscript">发送</button>
            <button class="transcript-action muted" @click="speech.clearTranscript()">清除</button>
          </div>
        </footer>
      </main>
    </template>

    <!-- ========== 小窗模式：浮动窗口 ========== -->
    <div
      v-if="isMini"
      class="mini-overlay"
      :class="{ dragging: isDragging, resizing: isResizing }"
    >
      <div
        class="mini-window"
        :style="miniStyle"
        ref="miniRef"
      >
        <!-- 可拖拽标题栏 -->
        <header class="mini-titlebar" @mousedown.prevent="startDrag">
          <span class="mini-title">◈ AI 助手</span>
          <div class="mini-title-info">
            <span v-if="configStore.activeProviderName" class="mini-chip">{{ configStore.activeProviderName }}</span>
            <span v-if="configStore.chatMode === 'RAG'" class="mini-chip mode">知识库</span>
          </div>
          <div class="mini-title-actions">
            <button class="mini-act" @click="exitMiniMode" title="展开完整窗口">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M15 3h6v6"/><path d="M10 14L21 3"/><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
              </svg>
            </button>
          </div>
        </header>

        <!-- 消息列表 -->
        <div class="mini-chat-body">
          <div v-if="chatStore.messages.length === 0" class="mini-empty">
            <p class="mini-empty-title">AI 助手</p>
            <p class="mini-empty-desc">输入问题开始对话</p>
          </div>
          <div class="mini-messages" v-else>
            <div v-for="(msg, idx) in chatStore.messages" :key="idx" class="mini-msg">
              <div :class="['mini-bubble', msg.role === 'USER' ? 'user' : 'ai']" v-html="renderMiniContent(msg.content)" />
            </div>
            <div ref="miniBottomRef" />
          </div>
        </div>

        <!-- 输入区域 -->
        <footer class="mini-input-area">
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

        <!-- 四角 resize handles -->
        <div class="resize-nw" @mousedown.prevent="startResize($event, 'nw')" />
        <div class="resize-ne" @mousedown.prevent="startResize($event, 'ne')" />
        <div class="resize-sw" @mousedown.prevent="startResize($event, 'sw')" />
        <div class="resize-se" @mousedown.prevent="startResize($event, 'se')" />
        <div class="resize-n" @mousedown.prevent="startResize($event, 'n')" />
        <div class="resize-s" @mousedown.prevent="startResize($event, 's')" />
        <div class="resize-e" @mousedown.prevent="startResize($event, 'e')" />
        <div class="resize-w" @mousedown.prevent="startResize($event, 'w')" />
      </div>
    </div>

    <ProviderDialog v-if="showProviderDialog" @close="showProviderDialog = false" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Setting } from '@element-plus/icons-vue'
import { useChatStore } from '../stores/chatStore'
import { useConfigStore } from '../stores/configStore'
import { useSpeech } from '../composables/useSpeech'
import { safeMarkdown } from '../marked-setup'
import ChatPanel from '../components/ChatPanel.vue'
import VoiceButton from '../components/VoiceButton.vue'
import SessionList from '../components/SessionList.vue'
import ProviderSelector from '../components/ProviderSelector.vue'
import ModeSwitch from '../components/ModeSwitch.vue'
import KnowledgePanel from '../components/KnowledgePanel.vue'
import ProviderDialog from '../components/ProviderDialog.vue'

const chatStore = useChatStore()
const configStore = useConfigStore()
const speech = useSpeech()

const textInput = ref('')
const sidebarTab = ref('chat')
const showProviderDialog = ref(false)
const inputRef = ref(null)
const miniRef = ref(null)
const miniBottomRef = ref(null)

const isMini = ref(false)

// --- 小窗拖拽 & 缩放状态 ---
const isDragging = ref(false)
const isResizing = ref(false)

const miniPos = reactive({ x: 0, y: 0 })
const miniSize = reactive({ w: 420, h: 560 })

const MIN_W = 320
const MIN_H = 400

// 拖拽临时记录
let dragStart = { mx: 0, my: 0, px: 0, py: 0 }
// 缩放临时记录
let resizeStart = { mx: 0, my: 0, px: 0, py: 0, pw: 0, ph: 0, dir: '' }

const miniStyle = computed(() => ({
  left: miniPos.x + 'px',
  top: miniPos.y + 'px',
  width: miniSize.w + 'px',
  height: miniSize.h + 'px'
}))

// --- 小窗 Markdown 渲染 ---
function renderMiniContent(content) {
  if (!content) return '<span style="color: var(--text-muted); font-style: italic;">思考中…</span>'
  return safeMarkdown(content)
}

// --- 进入/退出小窗 ---
function enterMiniMode() {
  const vw = window.innerWidth
  const vh = window.innerHeight
  miniPos.x = vw - miniSize.w - 20
  miniPos.y = vh - miniSize.h - 20
  isMini.value = true
}

function exitMiniMode() {
  isMini.value = false
}

// --- 拖拽 ---
function startDrag(e) {
  isDragging.value = true
  dragStart = { mx: e.clientX, my: e.clientY, px: miniPos.x, py: miniPos.y }
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

function onDrag(e) {
  const dx = e.clientX - dragStart.mx
  const dy = e.clientY - dragStart.my
  const vw = window.innerWidth
  const vh = window.innerHeight
  miniPos.x = Math.max(0, Math.min(dragStart.px + dx, vw - 80))
  miniPos.y = Math.max(0, Math.min(dragStart.py + dy, vh - 40))
}

function stopDrag() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// --- 缩放 ---
function startResize(e, dir) {
  isResizing.value = true
  resizeStart = {
    mx: e.clientX, my: e.clientY,
    px: miniPos.x, py: miniPos.y,
    pw: miniSize.w, ph: miniSize.h,
    dir
  }
  document.addEventListener('mousemove', onResize)
  document.addEventListener('mouseup', stopResize)
}

function onResize(e) {
  const dx = e.clientX - resizeStart.mx
  const dy = e.clientY - resizeStart.my
  const d = resizeStart.dir

  let nw = resizeStart.pw
  let nh = resizeStart.ph
  let nx = resizeStart.px
  let ny = resizeStart.py

  if (d.includes('e')) nw = Math.max(MIN_W, resizeStart.pw + dx)
  if (d.includes('w')) {
    nw = Math.max(MIN_W, resizeStart.pw - dx)
    nx = resizeStart.px + (resizeStart.pw - nw)
  }
  if (d.includes('s')) nh = Math.max(MIN_H, resizeStart.ph + dy)
  if (d.includes('n')) {
    nh = Math.max(MIN_H, resizeStart.ph - dy)
    ny = resizeStart.py + (resizeStart.ph - nh)
  }

  miniSize.w = nw
  miniSize.h = nh
  miniPos.x = nx
  miniPos.y = ny
}

function stopResize() {
  isResizing.value = false
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
}

// --- 小窗内消息滚动 ---
watch(() => chatStore.messages.length, () => {
  nextTick(() => miniBottomRef.value?.scrollIntoView({ behavior: 'smooth' }))
})
watch(() => {
  const msgs = chatStore.messages
  return msgs && msgs.length ? msgs[msgs.length - 1].content : ''
}, () => {
  nextTick(() => miniBottomRef.value?.scrollIntoView({ behavior: 'smooth' }))
})

// --- 生命周期 ---
onMounted(async () => {
  await configStore.loadProviders()
  await chatStore.loadSessions()
})

watch(() => speech.transcript.value, (val) => {
  if (val) textInput.value = val
})

function toggleSpeechMode() {
  speech.setMode(speech.mode.value === 'ptt' ? 'streaming' : 'ptt')
}

function sendText() {
  const content = textInput.value.trim()
  if (!content) return
  textInput.value = ''
  chatStore.sendMessage(content, configStore.chatMode, configStore.activeProviderId)
}

function sendTranscript() {
  const content = speech.transcript.value.trim()
  if (!content) return
  speech.clearTranscript()
  chatStore.sendMessage(content, configStore.chatMode, configStore.activeProviderId)
}
</script>

<style scoped>
.home { display: flex; height: 100vh; background: var(--bg-primary); }

/* ========== 完整模式 ========== */
.sidebar {
  width: 280px; flex-shrink: 0;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border);
  display: flex; flex-direction: column;
}
.sidebar-brand { display: flex; align-items: center; gap: 10px; padding: 20px 20px 16px; user-select: none; }
.brand-icon { font-size: 20px; color: var(--accent); }
.brand-text { font-size: 16px; font-weight: 700; color: var(--text-primary); letter-spacing: -0.3px; }
.sidebar-tabs { display: flex; gap: 4px; padding: 0 16px 12px; }
.tab-btn {
  flex: 1; padding: 8px 0; border-radius: var(--radius-sm);
  font-size: 13px; font-weight: 500; cursor: pointer;
  background: transparent; color: var(--text-muted); border: none; font-family: inherit;
  transition: all 0.2s ease;
}
.tab-btn:hover { background: var(--bg-hover); color: var(--text-secondary); }
.tab-btn.active { background: var(--accent-soft); color: var(--accent); }
.sidebar-content { flex: 1; overflow: hidden; }

.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }

.topbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 24px; flex-shrink: 0;
  border-bottom: 1px solid var(--border); background: var(--bg-secondary);
}
.topbar-left, .topbar-right { display: flex; align-items: center; gap: 10px; }
.mode-toggle { color: var(--text-muted) !important; font-size: 13px !important; }
.mode-toggle:hover { color: var(--text-primary) !important; }
.settings-btn { color: var(--text-muted) !important; }
.settings-btn:hover { color: var(--text-primary) !important; }

.input-area { padding: 16px 24px 20px; flex-shrink: 0; border-top: 1px solid var(--border); background: var(--bg-secondary); }
.input-row { display: flex; align-items: center; gap: 12px; }
.text-input-wrap { flex: 1; }
.text-input {
  width: 100%; padding: 12px 16px;
  background: var(--bg-input); border: 1px solid var(--border);
  border-radius: var(--radius-md); color: var(--text-primary);
  font-size: 14px; font-family: inherit; outline: none;
  transition: border-color 0.2s ease;
}
.text-input:focus { border-color: var(--accent); }
.text-input::placeholder { color: var(--text-muted); }
.text-input:disabled { opacity: 0.5; }

.send-btn {
  width: 44px; height: 44px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-md); border: none;
  cursor: pointer; transition: all 0.2s ease;
  background: var(--bg-input); color: var(--text-muted);
}
.send-btn.active { background: var(--accent); color: #fff; }
.send-btn.active:hover { background: var(--accent-hover); }
.send-btn:disabled { cursor: not-allowed; opacity: 0.4; }

.mini-toggle-btn {
  width: 44px; height: 44px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-md); border: none;
  cursor: pointer; transition: all 0.2s ease;
  background: var(--bg-input); color: var(--text-muted);
}
.mini-toggle-btn:hover { background: var(--bg-hover); color: var(--accent); }

.transcript-preview {
  margin-top: 10px; padding: 10px 14px;
  background: var(--accent-soft); border: 1px solid rgba(34,197,94,0.2);
  border-radius: var(--radius-sm); font-size: 14px;
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
}
.final-text { color: var(--text-primary); }
.interim-text { color: var(--text-muted); font-style: italic; }
.transcript-action { background: none; border: none; color: var(--accent); font-size: 13px; cursor: pointer; font-family: inherit; }
.transcript-action.muted { color: var(--text-muted); }
.transcript-action:hover { text-decoration: underline; }

/* ========== 小窗模式：浮动窗口 ========== */
.mini-overlay {
  position: fixed; inset: 0; z-index: 1000;
  pointer-events: none;
}
.mini-overlay.dragging, .mini-overlay.resizing { pointer-events: all; }

.mini-window {
  position: fixed;
  display: flex; flex-direction: column;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: 0 8px 40px rgba(0,0,0,0.5), 0 0 0 1px rgba(255,255,255,0.05) inset;
  overflow: hidden;
  pointer-events: all;
  user-select: none;
}

/* 标题栏 */
.mini-titlebar {
  display: flex; align-items: center; gap: 8px;
  padding: 0 10px 0 14px; height: 34px; flex-shrink: 0;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border);
  cursor: grab;
}
.mini-titlebar:active { cursor: grabbing; }
.mini-title { font-size: 12px; font-weight: 700; color: var(--text-secondary); white-space: nowrap; }
.mini-title-info { display: flex; align-items: center; gap: 6px; flex: 1; }
.mini-chip {
  font-size: 10px; padding: 1px 6px; border-radius: 8px;
  background: var(--bg-hover); color: var(--text-muted);
}
.mini-chip.mode {
  background: rgba(34,197,94,0.15); color: var(--accent);
}
.mini-title-actions { display: flex; gap: 2px; margin-left: auto; }
.mini-act {
  width: 26px; height: 26px; display: flex; align-items: center; justify-content: center;
  background: transparent; border: none; color: var(--text-muted); cursor: pointer;
  border-radius: 4px; transition: all 0.15s ease;
}
.mini-act:hover { background: var(--bg-hover); color: var(--text-primary); }

/* 消息体 */
.mini-chat-body { flex: 1; overflow-y: auto; padding: 10px 12px; }
.mini-empty { text-align: center; padding-top: 35%; opacity: 0.5; }
.mini-empty-title { font-size: 14px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.mini-empty-desc { font-size: 12px; color: var(--text-muted); }
.mini-msg { margin-bottom: 12px; }
.mini-bubble {
  max-width: 95%; padding: 8px 12px; border-radius: 10px;
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

/* 小窗内 markdown */
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
.mini-bubble :deep(pre code) { background: transparent; padding: 0; color: inherit; }
.mini-bubble :deep(blockquote) {
  border-left: 2px solid var(--accent); margin: 6px 0; padding: 4px 8px;
  background: rgba(34,197,94,0.05); border-radius: 0 6px 6px 0;
  color: var(--text-secondary); font-size: 12px;
}
.mini-bubble :deep(a) { color: var(--accent); }

/* 输入 */
.mini-input-area { padding: 8px 10px; flex-shrink: 0; border-top: 1px solid var(--border); background: var(--bg-secondary); }
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

/* ===== Resize handles ===== */
.resize-se, .resize-sw, .resize-ne, .resize-nw,
.resize-n, .resize-s, .resize-e, .resize-w {
  position: absolute; z-index: 10;
}
.resize-se, .resize-sw, .resize-ne, .resize-nw { width: 16px; height: 16px; }
.resize-n, .resize-s { width: 100%; height: 6px; left: 0; }
.resize-e, .resize-w { width: 6px; height: 100%; top: 0; }

.resize-se { bottom: 0; right: 0; cursor: nwse-resize; }
.resize-sw { bottom: 0; left: 0; cursor: nesw-resize; }
.resize-ne { top: 0; right: 0; cursor: nesw-resize; }
.resize-nw { top: 0; left: 0; cursor: nwse-resize; }
.resize-n { top: -3px; cursor: ns-resize; }
.resize-s { bottom: -3px; cursor: ns-resize; }
.resize-e { right: -3px; cursor: ew-resize; }
.resize-w { left: -3px; cursor: ew-resize; }
</style>
