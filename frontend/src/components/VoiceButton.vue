<template>
  <div class="voice-wrap">
    <button
      v-if="isSupported"
      :class="['voice-btn', { listening: isListening }]"
      @mousedown="isPTT ? $emit('start') : null"
      @mouseup="isPTT ? $emit('stop') : null"
      @mouseleave="isPTT ? $emit('stop') : null"
      @touchstart.prevent="isPTT ? $emit('start') : null"
      @touchend.prevent="isPTT ? $emit('stop') : null"
      @click="!isPTT ? (isListening ? $emit('stop') : $emit('start')) : null"
      :title="mode === 'system' ? (isListening ? '停止捕获系统音频' : '开始捕获系统音频') : isPTT ? '按住说话，松开发送' : (isListening ? '点击停止' : '点击开始')"
    >
      <div class="mic-ring" :class="{ active: isListening }" />
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
        <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
        <line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/>
      </svg>
    </button>
    <div v-else class="unsupported">
      <el-tag type="danger" size="small">浏览器不支持语音识别</el-tag>
    </div>
    <div v-if="isListening" class="recording-info">
      <span class="rec-text">{{ mode === 'system' ? '捕获系统音频中' : isPTT ? '松开发送' : '监听中' }}</span>
      <span class="rec-time">{{ elapsed }}</span>
      <div class="volume-bars">
        <span v-for="i in 5" :key="i" class="bar" :style="{ animationDelay: i * 0.12 + 's' }" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
const props = defineProps({ isSupported: Boolean, isListening: Boolean, isPTT: Boolean, mode: String })
defineEmits(['start', 'stop'])

const elapsed = ref('00:00')
let timer = null
watch(() => props.isListening, (val) => {
  if (val) {
    let sec = 0
    timer = setInterval(() => { sec++; elapsed.value = formatTime(sec) }, 1000)
  } else {
    clearInterval(timer); timer = null; elapsed.value = '00:00'
  }
})
onUnmounted(() => clearInterval(timer))
function formatTime(s) { return String(Math.floor(s / 60)).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0') }
</script>

<style scoped>
.voice-wrap { display: flex; align-items: center; gap: 14px; flex-shrink: 0; }

.voice-btn {
  width: 44px; height: 44px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  border: 1px solid var(--border); background: var(--bg-input);
  color: var(--text-secondary); cursor: pointer;
  position: relative; transition: all 0.2s ease;
}
.voice-btn:hover { border-color: var(--border-light); color: var(--text-primary); }
.voice-btn.listening {
  color: var(--accent); border-color: var(--accent);
  box-shadow: 0 0 20px rgba(34,197,94,0.25);
}

.mic-ring {
  position: absolute; inset: -3px; border-radius: 50%;
  border: 2px solid transparent; transition: all 0.3s ease;
}
.mic-ring.active {
  border-color: rgba(34,197,94,0.3);
  animation: ring-pulse 1.5s ease-in-out infinite;
}
@keyframes ring-pulse {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.12); opacity: 0; }
}

.recording-info { display: flex; align-items: center; gap: 12px; }
.rec-text { color: var(--accent); font-size: 13px; font-weight: 500; animation: pulse 1.5s infinite; }
.rec-time { color: var(--text-muted); font-size: 12px; font-variant-numeric: tabular-nums; font-family: var(--font-mono); }

.volume-bars { display: flex; align-items: flex-end; gap: 3px; height: 18px; }
.volume-bars .bar {
  width: 3px; background: var(--accent); border-radius: 2px;
  animation: bounce 0.5s ease-in-out infinite alternate;
}
.volume-bars .bar:nth-child(odd) { animation-duration: 0.45s; }
.volume-bars .bar:nth-child(2), .volume-bars .bar:nth-child(4) { animation-duration: 0.6s; }
@keyframes bounce { from { height: 4px; } to { height: 16px; } }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
</style>
