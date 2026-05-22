import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as api from '../services/api'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref([])
  const currentSessionId = ref(null)
  const messages = ref([])
  const isStreaming = ref(false)
  const streamingContent = ref('')

  const currentSession = computed(() =>
    sessions.value.find(s => s.id === currentSessionId.value)
  )

  async function loadSessions() {
    sessions.value = await api.listSessions()
  }

  async function createNewSession() {
    const session = await api.createSession()
    sessions.value.unshift(session)
    switchSession(session.id)
    return session
  }

  async function switchSession(sessionId) {
    currentSessionId.value = sessionId
    messages.value = await api.getSessionMessages(sessionId)
  }

  async function deleteCurrentSession(id) {
    await api.deleteSession(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    if (currentSessionId.value === id) {
      currentSessionId.value = null
      messages.value = []
    }
  }

  async function sendMessage(content, mode, providerId) {
    if (!currentSessionId.value) {
      await createNewSession()
    }

    const userMsg = { role: 'USER', content, timestamp: new Date().toISOString() }
    messages.value.push(userMsg)

    const assistantMsg = { role: 'ASSISTANT', content: '', timestamp: new Date().toISOString(), streaming: true }
    messages.value.push(assistantMsg)

    isStreaming.value = true
    streamingContent.value = ''

    await api.sendMessageStream(
      { sessionId: currentSessionId.value, content, mode, providerId },
      {
        onToken(token) {
          streamingContent.value += token
          const last = messages.value[messages.value.length - 1]
          if (last) last.content = streamingContent.value
        },
        onDone() {
          const last = messages.value[messages.value.length - 1]
          if (last) delete last.streaming
          isStreaming.value = false
          streamingContent.value = ''
          loadSessions()
        },
        onError(err) {
          console.error('Stream error:', err)
          const last = messages.value[messages.value.length - 1]
          if (last) {
            last.content = '请求失败: ' + err.message
            delete last.streaming
          }
          isStreaming.value = false
        }
      }
    )
  }

  return {
    sessions, currentSessionId, messages, isStreaming, streamingContent,
    currentSession, loadSessions, createNewSession, switchSession,
    deleteCurrentSession, sendMessage
  }
})
