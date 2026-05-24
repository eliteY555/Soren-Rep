import { ref } from 'vue'

const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition

/**
 * Web Speech API wrapper — optimized for speed and accuracy.
 *
 * @param {Function} onText - callback(text) called on every recognition update.
 *        The consumer writes directly to the input, no watcher overhead.
 */
export function useSpeech(onText) {
  const isSupported = ref(!!SpeechRecognition)
  const isListening = ref(false)
  const mode = ref('ptt') // 'ptt' | 'streaming'
  const error = ref(null)

  let recognition = null
  let silenceTimer = null
  let fullText = ''
  const SILENCE_TIMEOUT = 2500 // auto-stop after 2.5s of silence (streaming mode)

  function createRecognition() {
    if (!SpeechRecognition) return null
    const rec = new SpeechRecognition()
    rec.lang = 'zh-CN'
    rec.interimResults = true
    rec.continuous = true      // always continuous → faster re-recognition
    rec.maxAlternatives = 1    // single best result → lower latency

    rec.onresult = (event) => {
      let interim = ''
      let final = ''
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const r = event.results[i]
        if (r.isFinal) {
          final += r[0].transcript
        } else {
          interim += r[0].transcript
        }
      }
      if (final) {
        fullText += final
        // Trim leading punctuation/spaces from accumulated text
        fullText = fullText.replace(/^[，,。.！!？?；;：:\s]+/, '')
      }

      // Push combined text to consumer — one direct call, no watcher latency
      const combined = fullText + interim
      if (onText) onText(combined)

      // Reset silence timer → auto-stop in streaming mode
      if (silenceTimer) clearTimeout(silenceTimer)
      if (isListening.value && mode.value === 'streaming') {
        silenceTimer = setTimeout(() => {
          if (isListening.value) stopListening()
        }, SILENCE_TIMEOUT)
      }
    }

    rec.onerror = (event) => {
      if (event.error === 'no-speech' || event.error === 'aborted') {
        if (mode.value === 'streaming' && isListening.value) {
          setTimeout(() => {
            try { rec.start() } catch (_) { isListening.value = false }
          }, 300)
          return
        }
        if (event.error === 'aborted') return // normal PTT stop
      }
      error.value = event.error
      isListening.value = false
    }

    rec.onend = () => {
      if (silenceTimer) clearTimeout(silenceTimer)
      if (mode.value === 'streaming' && isListening.value) {
        setTimeout(() => {
          try { rec.start() } catch (_) { isListening.value = false }
        }, 200)
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
    // PTT: fresh start each press
    if (mode.value === 'ptt') {
      fullText = ''
      if (onText) onText('')
    }
    // Streaming: keep previous text, continue accumulating

    recognition = createRecognition()
    if (recognition) {
      try {
        recognition.start()
        isListening.value = true
      } catch (e) {
        error.value = 'start failed'
      }
    }
  }

  function stopListening() {
    isListening.value = false
    if (silenceTimer) clearTimeout(silenceTimer)
    if (recognition) {
      try { recognition.stop() } catch (_) { /* already stopped */ }
    }
  }

  function setMode(newMode) {
    mode.value = newMode
    if (recognition) {
      try { recognition.stop() } catch (_) { /* ignore */ }
      isListening.value = false
    }
    fullText = ''
    if (onText) onText('')
  }

  return {
    isSupported, isListening, mode, error,
    startListening, stopListening, setMode
  }
}
