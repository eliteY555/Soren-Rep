<template>
  <div class="voice-button-wrap">
    <!-- PTT mode: press-and-hold | Streaming mode: toggle on click -->
    <el-button
      v-if="isSupported"
      :type="isListening ? 'danger' : 'primary'"
      circle
      size="large"
      :class="{ recording: isListening }"
      @mousedown="isPTT ? $emit('start') : null"
      @mouseup="isPTT ? $emit('stop') : null"
      @mouseleave="isPTT ? $emit('stop') : null"
      @touchstart.prevent="isPTT ? $emit('start') : null"
      @touchend.prevent="isPTT ? $emit('stop') : null"
      @click="!isPTT ? (isListening ? $emit('stop') : $emit('start')) : null"
    >
      <el-icon :size="20"><Microphone /></el-icon>
    </el-button>
    <el-tag v-else type="warning">浏览器不支持语音识别，请使用 Chrome</el-tag>

    <div v-if="isListening" class="recording-indicator">
      <span class="recording-hint">{{ isPTT ? '正在录音，松开发送...' : '实时监听中...点击停止' }}</span>
      <span class="recording-timer">{{ elapsed }}</span>
      <!-- Volume bars -->
      <div class="volume-bars">
        <span v-for="i in 5" :key="i" class="bar" :style="{ animationDelay: i * 0.1 + 's' }" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { Microphone } from '@element-plus/icons-vue'

const props = defineProps({ isSupported: Boolean, isListening: Boolean, isPTT: Boolean })
defineEmits(['start', 'stop'])

const elapsed = ref('00:00')
let timer = null

watch(() => props.isListening, (val) => {
  if (val) {
    let sec = 0
    timer = setInterval(() => { sec++; elapsed.value = formatTime(sec) }, 1000)
  } else {
    clearInterval(timer)
    timer = null
    elapsed.value = '00:00'
  }
})

onUnmounted(() => clearInterval(timer))

function formatTime(s) { return String(Math.floor(s / 60)).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0') }
</script>

<style scoped>
.voice-button-wrap { display: flex; align-items: center; gap: 14px; }
.recording-indicator { display: flex; align-items: center; gap: 12px; }
.recording-hint { color: #f56c6c; font-size: 14px; animation: pulse 1.5s infinite; }
.recording-timer { color: #909399; font-size: 13px; font-variant-numeric: tabular-nums; }
.volume-bars { display: flex; align-items: flex-end; gap: 3px; height: 20px; }
.volume-bars .bar {
  width: 3px; background: #f56c6c; border-radius: 2px;
  animation: bounce 0.6s ease-in-out infinite alternate;
  height: 6px;
}
.volume-bars .bar:nth-child(odd) { animation-duration: 0.5s; }
.volume-bars .bar:nth-child(2), .volume-bars .bar:nth-child(4) { animation-duration: 0.7s; }
@keyframes bounce { to { height: 18px; } }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
</style>
