import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as api from '../services/api'

// Unique window ID — prevents reacting to own broadcasts
const winId = Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 6)

// BroadcastChannel for cross-window sync (main window ↔ popup)
let syncChannel = null
try {
  syncChannel = new BroadcastChannel('ai-assistant-sync')
} catch (_) { /* BroadcastChannel not supported */ }

export const useChatStore = defineStore('chat', () => {
  const sessions = ref([])
  const currentSessionId = ref(null)
  const messages = ref([])
  const isStreaming = ref(false)
  const streamingContent = ref('')

  const currentSession = computed(() =>
    sessions.value.find(s => s.id === currentSessionId.value)
  )

  // --- Broadcast sync helpers ---
  function broadcast(type) {
    if (syncChannel) {
      syncChannel.postMessage({ type, sessionId: currentSessionId.value, _from: winId })
    }
  }

  async function handleSyncEvent(event) {
    // Ignore own broadcasts
    if (event.data._from === winId) return

    const { type, sessionId } = event.data
    try {
      switch (type) {
        case 'mini-ready':
          if (currentSessionId.value) broadcast('session-switched')
          break
        case 'message-sent':
        case 'force-refresh':
          if (currentSessionId.value) {
            messages.value = await api.getSessionMessages(currentSessionId.value)
          }
          break
        case 'session-switched':
          if (sessionId && currentSessionId.value !== sessionId) {
            currentSessionId.value = sessionId
            messages.value = await api.getSessionMessages(sessionId)
          }
          break
        case 'session-deleted':
          await loadSessions()
          break
      }
    } catch (e) {
      console.warn('Sync error:', e)
    }
  }

  // Listen for sync from other window
  if (syncChannel) {
    syncChannel.onmessage = handleSyncEvent
  }

  // --- Actions ---
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
    sessionStorage.setItem('ai-active-session', sessionId)
    broadcast('session-switched')
  }

  async function deleteCurrentSession(id) {
    await api.deleteSession(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    if (currentSessionId.value === id) {
      currentSessionId.value = null
      messages.value = []
    }
    broadcast('session-deleted')
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
          // Progressive update — MessageBubble uses safeStreamingMarkdown during streaming
          const arr = [...messages.value]
          arr[aiIndex] = { ...arr[aiIndex], content: streamingContent.value }
          messages.value = arr
        },
        onDone() {
          const arr = [...messages.value]
          arr[aiIndex] = { ...arr[aiIndex], content: streamingContent.value, streaming: false }
          messages.value = arr
          isStreaming.value = false
          streamingContent.value = ''
          loadSessions()
          broadcast('message-sent')
        },
        onError(err) {
          console.error('Stream error:', err)
          const arr = [...messages.value]
          arr[aiIndex] = { ...arr[aiIndex], content: '请求失败: ' + err.message, streaming: false }
          messages.value = arr
          isStreaming.value = false
        }
      }
    )
  }

  return {
    sessions, currentSessionId, messages, isStreaming, streamingContent,
    currentSession, loadSessions, createNewSession, switchSession,
    deleteCurrentSession, sendMessage, broadcast
  }
})
