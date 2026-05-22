<template>
  <div class="session-list">
    <el-button type="primary" @click="$emit('newSession')" :icon="Plus" block>
      新对话
    </el-button>
    <div class="sessions">
      <div
        v-for="session in sessions" :key="session.id"
        :class="['session-item', { active: session.id === currentId }]"
        @click="$emit('switch', session.id)"
      >
        <span class="session-title">{{ session.title || '新对话' }}</span>
        <el-button :icon="Delete" circle size="small" text
          @click.stop="$emit('delete', session.id)" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, Delete } from '@element-plus/icons-vue'
defineProps({ sessions: Array, currentId: String })
defineEmits(['newSession', 'switch', 'delete'])
</script>

<style scoped>
.session-list { padding: 12px; }
.sessions { margin-top: 12px; }
.session-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 12px; border-radius: 6px; cursor: pointer; margin-bottom: 4px; font-size: 14px;
}
.session-item:hover { background: #f0f2f5; }
.session-item.active { background: #ecf5ff; color: #409eff; }
.session-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
</style>
