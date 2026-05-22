<template>
  <div :class="['home', { mini: isMini }]">
    <!-- Sidebar -->
    <aside v-show="!isMini" class="sidebar">
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
      <header v-show="!isMini" class="topbar">
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

      <!-- Mini mode: compact title bar -->
      <header v-show="isMini" class="mini-bar">
        <span class="mini-bar-brand">◈ AI 助手</span>
        <div class="mini-bar-actions">
          <span v-if="configStore.activeProviderName" class="mini-bar-provider">{{ configStore.activeProviderName }}</span>
          <span v-if="configStore.chatMode === 'RAG'" class="mini-bar-mode">知识库</span>
          <button class="mini-expand-btn" @click="isMini = false" title="展开完整窗口">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M15 3h6v6"/><path d="M10 14L21 3"/><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
            </svg>
          </button>
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

          <button
            class="mini-toggle-btn"
            @click="isMini = !isMini"
            :title="isMini ? '展开完整窗口' : '小窗模式'"
          >
            <svg v-if="!isMini" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="4 8 4 4 8 4"/><line x1="20" y1="4" x2="12" y2="12"/>
              <polyline points="4 20 4 16"/><polyline points="20 20 20 16"/>
            </svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M15 3h6v6"/><path d="M10 14L21 3"/><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
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

    <ProviderDialog v-if="showProviderDialog" @close="showProviderDialog = false" />
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { Setting } from '@element-plus/icons-vue'
import { useChatStore } from '../stores/chatStore'
import { useConfigStore } from '../stores/configStore'
import { useSpeech } from '../composables/useSpeech'
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
const isMini = ref(false)

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

/* Sidebar */
.sidebar {
  width: 280px; flex-shrink: 0;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border);
  display: flex; flex-direction: column;
}
.sidebar-brand {
  display: flex; align-items: center; gap: 10px;
  padding: 20px 20px 16px; user-select: none;
}
.brand-icon { font-size: 20px; color: var(--accent); }
.brand-text { font-size: 16px; font-weight: 700; color: var(--text-primary); letter-spacing: -0.3px; }
.sidebar-tabs {
  display: flex; gap: 4px; padding: 0 16px 12px;
}
.tab-btn {
  flex: 1; padding: 8px 0; border-radius: var(--radius-sm);
  font-size: 13px; font-weight: 500; cursor: pointer;
  background: transparent; color: var(--text-muted);
  border: none; font-family: inherit;
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
  border-bottom: 1px solid var(--border);
  background: var(--bg-secondary);
}
.topbar-left, .topbar-right { display: flex; align-items: center; gap: 10px; }
.mode-toggle { color: var(--text-muted) !important; font-size: 13px !important; }
.mode-toggle:hover { color: var(--text-primary) !important; }
.settings-btn { color: var(--text-muted) !important; }
.settings-btn:hover { color: var(--text-primary) !important; }

/* Mini mode bar */
.mini-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 6px 14px; flex-shrink: 0;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border);
}
.mini-bar-brand { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.mini-bar-actions { display: flex; align-items: center; gap: 8px; }
.mini-bar-provider { font-size: 11px; color: var(--text-muted); }
.mini-bar-mode {
  font-size: 10px; padding: 1px 6px; border-radius: 8px;
  background: rgba(34,197,94,0.15); color: var(--accent);
}
.mini-expand-btn {
  width: 28px; height: 28px; display: flex; align-items: center; justify-content: center;
  background: transparent; border: 1px solid var(--border); border-radius: 4px;
  color: var(--text-muted); cursor: pointer; transition: all 0.15s ease;
}
.mini-expand-btn:hover { background: var(--bg-hover); color: var(--accent); border-color: var(--accent); }

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

/* Mini toggle button */
.mini-toggle-btn {
  width: 44px; height: 44px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-md); border: none;
  cursor: pointer; transition: all 0.2s ease;
  background: var(--bg-input); color: var(--text-muted);
}
.mini-toggle-btn:hover { background: var(--bg-hover); color: var(--accent); }

/* Mini mode layout tweaks */
.home.mini .input-area { padding: 10px 14px 14px; }
.home.mini .text-input { font-size: 13px; padding: 10px 14px; }
.home.mini .send-btn { width: 38px; height: 38px; }
.home.mini .mini-toggle-btn { width: 38px; height: 38px; }

/* Transcript preview */
.transcript-preview {
  margin-top: 10px; padding: 10px 14px;
  background: var(--accent-soft); border: 1px solid rgba(34,197,94,0.2);
  border-radius: var(--radius-sm); font-size: 14px;
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
}
.final-text { color: var(--text-primary); }
.interim-text { color: var(--text-muted); font-style: italic; }
.transcript-action {
  background: none; border: none; color: var(--accent);
  font-size: 13px; cursor: pointer; font-family: inherit;
}
.transcript-action.muted { color: var(--text-muted); }
.transcript-action:hover { text-decoration: underline; }
</style>
