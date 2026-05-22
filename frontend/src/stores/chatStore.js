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
    messages.value = [...messages.value, userMsg]

    const assistantMsg = { role: 'ASSISTANT', content: '', timestamp: new Date().toISOString(), streaming: true }
    messages.value = [...messages.value, assistantMsg]

    isStreaming.value = true
    streamingContent.value = ''
    const aiIndex = messages.value.length - 1

    await api.sendMessageStream(
      { sessionId: currentSessionId.value, content, mode, providerId },
      {
        onToken(token) {
          streamingContent.value += token
          // Replace entire object to force Vue reactivity
          const arr = [...messages.value]
          arr[aiIndex] = { ...arr[aiIndex], content: streamingContent.value }
          messages.value = arr
        },
        onDone() {
          // Replace to remove streaming flag
          const arr = [...messages.value]
          const item = { ...arr[aiIndex] }
          delete item.streaming
          arr[aiIndex] = item
          messages.value = arr
          isStreaming.value = false
          streamingContent.value = ''
          loadSessions()
        },
        onError(err) {
          console.error('Stream error:', err)
          const arr = [...messages.value]
          arr[aiIndex] = { ...arr[aiIndex], content: '请求失败: ' + err.message, streaming: undefined }
          messages.value = arr
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
