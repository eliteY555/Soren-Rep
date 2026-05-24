<template>
  <div class="home">
    <!-- Sidebar -->
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

    <!-- Main area -->
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
            {{ speechMode === 'ptt' ? '按键说话' : speechMode === 'mic-stream' ? '实时麦克风' : '系统音频' }}
          </el-button>
          <el-button size="small" text :icon="Setting" @click="showProviderDialog = true" class="settings-btn" />
        </div>
      </header>

      <ChatPanel :messages="chatStore.messages" />

      <footer class="input-area">
        <div class="input-row">
          <VoiceButton
            :isSupported="(speechMode === 'system' ? sysSpeech.isSupported.value : micSpeech.isSupported.value)"
            :isListening="(speechMode === 'system' ? sysSpeech.isListening.value : micSpeech.isListening.value)"
            :isPTT="speechMode === 'ptt'"
            :mode="speechMode"
            @start="onVoiceStart()"
            @stop="onVoiceStop()"
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

          <button
            class="mini-toggle-btn"
            @click="openMiniWindow"
            :title="miniOpen ? '小窗已打开' : '弹出小窗'"
            :class="{ active: miniOpen }"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M15 3h6v6"/><path d="M10 14L21 3"/><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
            </svg>
          </button>
        </div>

      </footer>
    </main>

    <ProviderDialog v-if="showProviderDialog" @close="showProviderDialog = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Setting } from '@element-plus/icons-vue'
import { useChatStore } from '../stores/chatStore'
import { useConfigStore } from '../stores/configStore'
import { useSpeech } from '../composables/useSpeech'
import { useSystemSpeech } from '../composables/useSystemSpeech'
import ChatPanel from '../components/ChatPanel.vue'
import VoiceButton from '../components/VoiceButton.vue'
import SessionList from '../components/SessionList.vue'
import ProviderSelector from '../components/ProviderSelector.vue'
import ModeSwitch from '../components/ModeSwitch.vue'
import KnowledgePanel from '../components/KnowledgePanel.vue'
import ProviderDialog from '../components/ProviderDialog.vue'

const chatStore = useChatStore()
const configStore = useConfigStore()

const textInput = ref('')
const sidebarTab = ref('chat')
const showProviderDialog = ref(false)
const inputRef = ref(null)
const miniOpen = ref(false)

let miniWindow = null
let closeWatcher = null

// Three voice modes: ptt (mic push-to-talk), mic-stream (mic continuous), system (output audio)
const speechMode = ref('ptt')

const micSpeech = useSpeech((text) => { textInput.value = text })
const sysSpeech = useSystemSpeech((text) => { textInput.value = text })

function openMiniWindow() {
  if (miniWindow && !miniWindow.closed) {
    miniWindow.focus()
    return
  }

  const w = 420
  const h = 620
  const left = screen.availWidth - w - 20
  const top = screen.availHeight - h - 60

  miniWindow = window.open(
    `${window.location.origin}?mini=1`,
    'ai-assistant-mini',
    `width=${w},height=${h},left=${left},top=${top},resizable=yes,menubar=no,toolbar=no,location=no,status=no`
  )

  if (miniWindow) {
    miniOpen.value = true
    closeWatcher = setInterval(() => {
      if (miniWindow.closed) {
        miniOpen.value = false
        miniWindow = null
        clearInterval(closeWatcher)
        closeWatcher = null
      }
    }, 500)
  }
}

onMounted(async () => {
  await configStore.loadProviders()
  await chatStore.loadSessions()
})

onUnmounted(() => {
  if (closeWatcher) clearInterval(closeWatcher)
})

function toggleSpeechMode() {
  // Cycle: ptt → mic-stream → system → ptt
  if (speechMode.value === 'ptt') {
    micSpeech.setMode('streaming')
    speechMode.value = 'mic-stream'
  } else if (speechMode.value === 'mic-stream') {
    // Stop mic before switching to system mode
    if (micSpeech.isListening.value) micSpeech.stopListening()
    speechMode.value = 'system'
  } else {
    if (sysSpeech.isListening.value) sysSpeech.stopListening()
    micSpeech.setMode('ptt')
    speechMode.value = 'ptt'
  }
  textInput.value = ''
}

function onVoiceStart() {
  if (speechMode.value === 'system') {
    sysSpeech.startListening()
  } else {
    micSpeech.startListening()
  }
}

function onVoiceStop() {
  if (speechMode.value === 'system') {
    sysSpeech.stopListening()
  } else {
    micSpeech.stopListening()
  }
}

function sendText() {
  const content = textInput.value.trim()
  if (!content) return
  textInput.value = ''
  chatStore.sendMessage(content, configStore.chatMode, configStore.activeProviderId)
}
</script>

<style scoped>
.home { display: flex; height: 100vh; background: var(--bg-primary); }

/* Sidebar */
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

/* Main */
.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }

/* Topbar */
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

/* Input area */
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

/* Mini toggle */
.mini-toggle-btn {
  width: 44px; height: 44px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-md); border: none;
  cursor: pointer; transition: all 0.2s ease;
  background: var(--bg-input); color: var(--text-muted);
}
.mini-toggle-btn:hover { background: var(--bg-hover); color: var(--accent); }
.mini-toggle-btn.active { background: var(--accent-soft); color: var(--accent); }

</style>
