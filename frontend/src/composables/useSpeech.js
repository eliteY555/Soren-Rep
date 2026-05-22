import { ref, onUnmounted } from 'vue'

const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition

export function useSpeech() {
  const isSupported = ref(!!SpeechRecognition)
  const isListening = ref(false)
  const transcript = ref('')
  const interimTranscript = ref('')
  const mode = ref('ptt') // 'ptt' | 'streaming'
  const error = ref(null)

  let recognition = null

  function createRecognition() {
    if (!SpeechRecognition) return null
    const rec = new SpeechRecognition()
    rec.lang = 'zh-CN'
    rec.interimResults = true
    rec.continuous = mode.value === 'streaming'
    rec.maxAlternatives = 1

    rec.onresult = (event) => {
      let interim = ''
      let final = ''
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i]
        if (result.isFinal) {
          final += result[0].transcript
        } else {
          interim += result[0].transcript
        }
      }
      if (final) transcript.value += final
      interimTranscript.value = interim
    }

    rec.onerror = (event) => {
      // 'no-speech' and 'aborted' are normal in streaming mode, don't treat as errors
      if (event.error === 'no-speech' || event.error === 'aborted') {
        if (mode.value === 'streaming' && isListening.value) {
          try { rec.start() } catch (e) { /* ignore */ }
          return
        }
      }
      error.value = event.error
      isListening.value = false
    }

    rec.onend = () => {
      if (mode.value === 'streaming' && isListening.value) {
        try { rec.start() } catch (e) { isListening.value = false }
      } else {
        isListening.value = false
      }
    }

    return rec
  }

  function startListening() {
    if (!isSupported.value) {
      error.value = '浏览器不支持语音识别'
      return
    }
    error.value = null
    // In PTT mode, clear transcript for fresh start
    if (mode.value === 'ptt') {
      transcript.value = ''
    }
    interimTranscript.value = ''

    recognition = createRecognition()
    if (recognition) {
      recognition.start()
      isListening.value = true
    }
  }

  function stopListening() {
    isListening.value = false
    if (recognition) {
      recognition.stop()
    }
  }

  function setMode(newMode) {
    mode.value = newMode
    if (recognition) {
      recognition.stop()
      isListening.value = false
    }
  }

  function clearTranscript() {
    transcript.value = ''
    interimTranscript.value = ''
  }

  onUnmounted(() => {
    isListening.value = false
    if (recognition) recognition.stop()
  })

  return {
    isSupported, isListening, transcript, interimTranscript, mode, error,
    startListening, stopListening, setMode, clearTranscript
  }
}
