<template>
  <div class="chat-panel" ref="panelRef">
    <div v-if="messages.length === 0" class="empty-state">
      <h2>语音 AI 助手</h2>
      <p>按住麦克风按钮开始语音提问，或直接在下方输入文字</p>
    </div>
    <MessageBubble v-for="(msg, idx) in messages" :key="idx" v-bind="msg" />
    <div ref="bottomRef" />
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import MessageBubble from './MessageBubble.vue'

const props = defineProps({ messages: Array })
const bottomRef = ref(null)

watch(() => props.messages?.length, () => {
  nextTick(() => bottomRef.value?.scrollIntoView({ behavior: 'smooth' }))
}, { deep: true })
</script>

<style scoped>
.chat-panel { flex: 1; overflow-y: auto; padding: 20px; }
.empty-state { text-align: center; margin-top: 20vh; color: #909399; }
.empty-state h2 { margin-bottom: 8px; color: #303133; }
</style>
