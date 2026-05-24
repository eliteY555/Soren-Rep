import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// --- Chat API ---

export function sendMessageStream(chatRequest, { onToken, onDone, onError }) {
  let finished = false

  return fetch('/api/chat/send', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(chatRequest)
  }).then(response => {
    if (!response.ok) throw new Error(`HTTP ${response.status}`)

    // Wrap stream reading in a Promise that resolves only when the stream
    // completes (SSE event:done or reader done), NOT when the first chunk arrives.
    return new Promise((resolve, reject) => {
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      function read() {
        if (finished) return
        reader.read().then(({ done, value }) => {
          if (finished) return
          if (done) {
            finished = true
            onDone()
            resolve()
            return
          }
          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''
          let currentEvent = null
          for (const line of lines) {
            if (line.startsWith('event:')) {
              currentEvent = line.slice(6).trim()
            } else if (line.startsWith('data:')) {
              const data = line.slice(5).trim()
              if (currentEvent === 'token') {
                onToken(data)
              } else if (currentEvent === 'done' || data === '[DONE]') {
                finished = true
                onDone()
                resolve()
                return
              }
            }
          }
          read()
        }).catch(err => {
          if (!finished) {
            finished = true
            onError(err)
            reject(err)
          }
        })
      }
      read()
    })
  }).catch(onError)
}

export function listSessions() {
  return http.get('/chat/sessions').then(r => r.data.data)
}

export function createSession() {
  return http.post('/chat/sessions').then(r => r.data.data)
}

export function deleteSession(id) {
  return http.delete(`/chat/sessions/${id}`).then(r => r.data)
}

export function getSessionMessages(id) {
  return http.get(`/chat/sessions/${id}/messages`).then(r => r.data.data)
}

// --- Config API ---

export function listProviders() {
  return http.get('/config/providers').then(r => r.data.data)
}

export function addProvider(data) {
  return http.post('/config/providers', data).then(r => r.data.data)
}

export function updateProvider(id, data) {
  return http.put(`/config/providers/${id}`, data).then(r => r.data.data)
}

export function deleteProvider(id) {
  return http.delete(`/config/providers/${id}`).then(r => r.data)
}

export function activateProvider(id) {
  return http.put(`/config/providers/${id}/activate`).then(r => r.data.data)
}

// --- Knowledge Base API ---

export function uploadDocument(file) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/knowledge/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(r => r.data.data)
}

export function listDocuments() {
  return http.get('/knowledge/documents').then(r => r.data.data)
}

export function deleteDocument(id) {
  return http.delete(`/knowledge/documents/${id}`).then(r => r.data)
}

export function searchKnowledge(query, topK = 5) {
  return http.post('/knowledge/search', null, {
    params: { query, topK }
  }).then(r => r.data.data)
}
