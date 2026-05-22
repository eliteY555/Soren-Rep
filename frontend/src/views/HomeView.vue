<template>
  <div class="home">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-tabs">
        <el-button
          :type="sidebarTab === 'chat' ? 'primary' : ''"
          size="small"
          @click="sidebarTab = 'chat'"
        >对话</el-button>
        <el-button
          :type="sidebarTab === 'knowledge' ? 'primary' : ''"
          size="small"
          @click="sidebarTab = 'knowledge'"
        >知识库</el-button>
      </div>
      <SessionList
        v-if="sidebarTab === 'chat'"
        :sessions="chatStore.sessions"
        :currentId="chatStore.currentSessionId"
        @newSession="chatStore.createNewSession()"
        @switch="chatStore.switchSession($event)"
        @delete="chatStore.deleteCurrentSession($event)"
      />
      <KnowledgePanel v-if="sidebarTab === 'knowledge'" />
    </aside>

    <!-- Main area -->
    <main class="main">
      <!-- Top bar -->
      <header class="topbar">
        <ProviderSelector
          :providers="configStore.providers"
          :activeId="configStore.activeProviderId"
          @change="configStore.activateProvider($event)"
        />
        <ModeSwitch
          :mode="configStore.chatMode"
          @change="configStore.setChatMode($event)"
        />
        <el-button
          @click="speech.mode.value === 'ptt' ? speech.setMode('streaming') : speech.setMode('ptt')"
          size="small"
        >
          {{ speech.mode.value === 'ptt' ? '切换到实时模式' : '切换到按键模式' }}
        </el-button>
      </header>

      <!-- Chat panel -->
      <ChatPanel :messages="chatStore.messages" />

      <!-- Input area -->
      <footer class="input-area">
        <div class="input-row">
          <VoiceButton
            :isSupported="speech.isSupported.value"
            :isListening="speech.isListening.value"
            @start="speech.startListening()"
            @stop="speech.stopListening()"
          />

          <el-input
            v-model="textInput"
            placeholder="输入问题，或按住麦克风按钮语音输入..."
            @keyup.enter="sendText"
            :disabled="chatStore.isStreaming"
            clearable
          />

          <el-button type="primary" :icon="Promotion"
            @click="sendText"
            :disabled="!textInput.trim() || chatStore.isStreaming">
            发送
          </el-button>
        </div>

        <!-- Voice transcript preview -->
        <div v-if="speech.transcript.value || speech.interimTranscript.value" class="transcript-preview">
          <span>{{ speech.transcript.value }}</span>
          <span style="color: #909399">{{ speech.interimTranscript.value }}</span>
          <el-button size="small" text @click="sendTranscript">发送语音文本</el-button>
          <el-button size="small" text @click="speech.clearTranscript()">清除</el-button>
        </div>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { Promotion } from '@element-plus/icons-vue'
import { useChatStore } from '../stores/chatStore'
import { useConfigStore } from '../stores/configStore'
import { useSpeech } from '../composables/useSpeech'
import ChatPanel from '../components/ChatPanel.vue'
import VoiceButton from '../components/VoiceButton.vue'
import SessionList from '../components/SessionList.vue'
import ProviderSelector from '../components/ProviderSelector.vue'
import ModeSwitch from '../components/ModeSwitch.vue'
import KnowledgePanel from '../components/KnowledgePanel.vue'

const chatStore = useChatStore()
const configStore = useConfigStore()
const speech = useSpeech()

const textInput = ref('')
const sidebarTab = ref('chat')

onMounted(async () => {
  await configStore.loadProviders()
  await chatStore.loadSessions()
})

// When speech recognition completes, auto-fill text input
watch(() => speech.transcript.value, (val) => {
  if (val) textInput.value = val
})

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
.home { display: flex; height: 100vh; }
.sidebar { width: 260px; border-right: 1px solid #e4e7ed; background: #fafafa; overflow-y: auto; }
.sidebar-tabs { display: flex; gap: 8px; padding: 12px 12px 0 12px; }
.main { flex: 1; display: flex; flex-direction: column; }
.topbar {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 20px; border-bottom: 1px solid #e4e7ed;
}
.input-area { border-top: 1px solid #e4e7ed; padding: 16px 20px; }
.input-row { display: flex; align-items: center; gap: 12px; }
.transcript-preview {
  margin-top: 8px; padding: 8px 12px; background: #f0f9eb;
  border-radius: 6px; font-size: 14px;
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
}
</style>
