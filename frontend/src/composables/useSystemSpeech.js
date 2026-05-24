import { ref, onUnmounted } from 'vue'

/**
 * System audio capture + backend STT via Qwen Paraformer.
 *
 * Captures computer audio OUTPUT (speakers/headphones) via getDisplayMedia,
 * converts to 16kHz mono PCM/WAV using AudioContext, sends chunks to backend.
 *
 * @param {Function} onText - callback(accumulatedText) on each recognition result
 */
export function useSystemSpeech(onText) {
  const isSupported = ref(false)
  const isListening = ref(false)
  const error = ref(null)

  let mediaStream = null
  let audioCtx = null
  let processor = null
  let fullText = ''
  let pcmBuffer = new Int16Array(0)
  const SAMPLE_RATE = 16000
  const CHUNK_MS = 600 // send every 600ms for low latency
  const SAMPLES_PER_CHUNK = SAMPLE_RATE * CHUNK_MS / 1000 // 9600 samples

  // Feature-detect
  function detect() {
    return !!(
      navigator.mediaDevices?.getDisplayMedia &&
      typeof AudioContext !== 'undefined'
    )
  }
  isSupported.value = detect()

  // --- WAV encoder: header (44 bytes) + raw PCM ---
  function encodeWAV(samples) {
    const numSamples = samples.length
    const buf = new ArrayBuffer(44 + numSamples * 2)
    const view = new DataView(buf)

    function wrt(s, off, len) {
      for (let i = 0; i < len; i++) {
        view.setUint8(off + i, s.charCodeAt(i))
      }
    }
    function u16(off, v) { view.setUint16(off, v, true) }
    function u32(off, v) { view.setUint32(off, v, true) }

    wrt('RIFF', 0, 4)
    u32(4, 36 + numSamples * 2)
    wrt('WAVE', 8, 4)
    wrt('fmt ', 12, 4)
    u32(16, 16)          // PCM
    u16(20, 1)           // mono
    u16(22, 1)           // 1 channel
    u32(24, SAMPLE_RATE)
    u32(28, SAMPLE_RATE * 2)
    u16(32, 2)
    u16(34, 16)
    wrt('data', 36, 4)
    u32(40, numSamples * 2)

    // Write PCM samples
    let offset = 44
    for (let i = 0; i < numSamples; i++) {
      const s = Math.max(-32768, Math.min(32767, samples[i]))
      view.setInt16(offset, s, true)
      offset += 2
    }
    return new Blob([buf], { type: 'audio/wav' })
  }

  // --- Send audio chunk to backend, async (fire-and-forget) ---
  async function sendChunk(wavBlob) {
    try {
      const fd = new FormData()
      fd.append('audio', wavBlob, 'chunk.wav')
      fd.append('format', 'wav')
      fd.append('language', 'zh')

      const res = await fetch('/api/speech/transcribe', { method: 'POST', body: fd })
      if (!res.ok) return
      const json = await res.json()
      const text = json?.data?.text || ''
      if (text) {
        fullText += text
        if (onText) onText(fullText)
      }
    } catch (_) { /* skip failed chunk */ }
  }

  // --- Flush accumulated PCM buffer as WAV ---
  function flushBuffer() {
    if (pcmBuffer.length === 0) return
    const wav = encodeWAV(pcmBuffer)
    pcmBuffer = new Int16Array(0)
    sendChunk(wav)
  }

  let flushTimer = null

  async function startListening() {
    if (!isSupported.value) {
      error.value = '浏览器不支持系统音频捕获'
      return
    }
    error.value = null
    fullText = ''
    pcmBuffer = new Int16Array(0)

    try {
      // Capture system audio output (screen + audio share)
      mediaStream = await navigator.mediaDevices.getDisplayMedia({
        audio: true,
        video: { width: 1, height: 1 } // minimal — required by some browsers
      })

      // Stop video track immediately
      mediaStream.getVideoTracks().forEach(t => t.stop())

      const audioTrack = mediaStream.getAudioTracks()[0]
      if (!audioTrack) {
        error.value = '未检测到系统音频输出（请勾选"共享系统音频"）'
        return
      }

      audioTrack.addEventListener('ended', () => stopListening())

      // AudioContext at target sample rate
      audioCtx = new AudioContext({ sampleRate: SAMPLE_RATE })
      const source = audioCtx.createMediaStreamSource(mediaStream)

      // ScriptProcessor: raw PCM samples (256 sample frames = ~16ms)
      processor = audioCtx.createScriptProcessor(256, 1, 1)
      // Avoid feedback — don't connect to destination
      source.connect(processor)

      processor.onaudioprocess = (event) => {
        if (!isListening.value) return
        const input = event.inputBuffer.getChannelData(0) // Float32Array [-1, 1]
        // Convert to Int16
        const int16 = new Int16Array(input.length)
        for (let i = 0; i < input.length; i++) {
          int16[i] = input[i] * 0x7FFF
        }
        // Append to buffer
        const combined = new Int16Array(pcmBuffer.length + int16.length)
        combined.set(pcmBuffer)
        combined.set(int16, pcmBuffer.length)
        pcmBuffer = combined

        // Flush when enough samples accumulated
        if (pcmBuffer.length >= SAMPLES_PER_CHUNK) {
          flushBuffer()
        }
      }

      // Periodic flush (catches trailing audio below threshold)
      flushTimer = setInterval(() => {
        if (pcmBuffer.length > SAMPLE_RATE * 0.3) { // > 300ms of audio
          flushBuffer()
        }
      }, CHUNK_MS)

      isListening.value = true
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
    if (flushTimer) { clearInterval(flushTimer); flushTimer = null }
    // Flush remaining audio
    if (pcmBuffer.length > SAMPLE_RATE * 0.2) flushBuffer()
    pcmBuffer = new Int16Array(0)

    if (processor) {
      try { processor.disconnect() } catch (_) { /* ok */ }
      processor = null
    }
    if (audioCtx) {
      audioCtx.close().catch(() => {})
      audioCtx = null
    }
    if (mediaStream) {
      mediaStream.getTracks().forEach(t => t.stop())
      mediaStream = null
    }
  }

  onUnmounted(() => stopListening())

  return { isSupported, isListening, error, startListening, stopListening }
}
