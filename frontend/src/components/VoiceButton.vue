<template>
  <div class="voice-button-wrap">
    <el-button
      v-if="isSupported"
      :type="isListening ? 'danger' : 'primary'"
      circle
      size="large"
      @mousedown="$emit('start')"
      @mouseup="$emit('stop')"
      @mouseleave="$emit('stop')"
      @touchstart.prevent="$emit('start')"
      @touchend.prevent="$emit('stop')"
    >
      <el-icon :size="20"><Microphone /></el-icon>
    </el-button>
    <el-tag v-else type="warning">浏览器不支持语音识别，请使用 Chrome</el-tag>
    <span v-if="isListening" class="recording-hint">正在录音，松开发送...</span>
  </div>
</template>

<script setup>
import { Microphone } from '@element-plus/icons-vue'
defineProps({ isSupported: Boolean, isListening: Boolean })
defineEmits(['start', 'stop'])
</script>

<style scoped>
.voice-button-wrap { display: flex; align-items: center; gap: 12px; }
.recording-hint { color: #f56c6c; font-size: 14px; animation: pulse 1.5s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
</style>
