import { ref, onUnmounted } from 'vue'

/**
 * System audio capture + backend STT transcription.
 *
 * Captures computer audio OUTPUT (speakers/headphones) via getDisplayMedia,
 * sends chunks to /api/speech/transcribe for Qwen Paraformer processing.
 *
 * Use case: voice chat — transcribe what the OTHER person is saying.
 *
 * @param {Function} onText - callback(accumulatedText) on each recognition result
 */
export function useSystemSpeech(onText) {
  const isSupported = ref(false)
  const isListening = ref(false)
  const error = ref(null)

  let mediaStream = null
  let mediaRecorder = null
  let fullText = ''
  let chunkTimer = null
  const CHUNK_MS = 1500 // send audio chunk every 1.5s

  // Feature-detect on creation
  function detect() {
    return !!(
      navigator.mediaDevices &&
      navigator.mediaDevices.getDisplayMedia &&
      typeof MediaRecorder !== 'undefined'
    )
  }
  isSupported.value = detect()

  async function startListening() {
    if (!isSupported.value) {
      error.value = '浏览器不支持系统音频捕获'
      return
    }
    error.value = null
    fullText = ''

    try {
      // Capture system audio output
      mediaStream = await navigator.mediaDevices.getDisplayMedia({
        audio: true,
        video: { width: 1, height: 1 } // minimal video — required by some browsers
      })

      // Stop any video tracks (we only need audio)
      mediaStream.getVideoTracks().forEach(t => t.stop())

      const audioTrack = mediaStream.getAudioTracks()[0]
      if (!audioTrack) {
        error.value = '未检测到系统音频输出'
        return
      }

      // Create dedicated audio stream
      const audioStream = new MediaStream([audioTrack])

      // Try WAV first, fall back to browser default
      let mimeType = 'audio/webm;codecs=opus'
      if (MediaRecorder.isTypeSupported('audio/webm;codecs=opus')) {
        mimeType = 'audio/webm;codecs=opus'
      } else if (MediaRecorder.isTypeSupported('audio/webm')) {
        mimeType = 'audio/webm'
      }

      mediaRecorder = new MediaRecorder(audioStream, { mimeType, audioBitsPerSecond: 16000 })

      mediaRecorder.ondataavailable = async (event) => {
        if (event.data.size < 100) return // skip tiny/silent chunks
        try {
          const formData = new FormData()
          formData.append('audio', event.data, 'chunk.webm')
          formData.append('language', 'zh')

          const res = await fetch('/api/speech/transcribe', {
            method: 'POST',
            body: formData
          })
          if (!res.ok) return
          const json = await res.json()
          const text = json?.data?.text || ''
          if (text) {
            fullText += text
            if (onText) onText(fullText)
          }
        } catch (e) {
          // Skip failed chunks — continue recording
        }
      }

      mediaRecorder.onerror = (e) => {
        error.value = 'recorder error'
        stopListening()
      }

      // Record in timeslice mode → ondataavailable every CHUNK_MS
      mediaRecorder.start(CHUNK_MS)
      isListening.value = true

      // Also keep track of audio track ending
      audioTrack.addEventListener('ended', () => stopListening())

    } catch (e) {
      if (e.name === 'AbortError' || e.name === 'NotAllowedError') {
        error.value = '需要授权系统音频共享权限'
      } else {
        error.value = e.message
      }
    }
  }

  function stopListening() {
    isListening.value = false
    if (chunkTimer) clearTimeout(chunkTimer)

    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      try { mediaRecorder.stop() } catch (_) { /* ok */ }
    }
    if (mediaStream) {
      mediaStream.getTracks().forEach(t => t.stop())
      mediaStream = null
    }
  }

  onUnmounted(() => stopListening())

  return { isSupported, isListening, error, startListening, stopListening }
}
