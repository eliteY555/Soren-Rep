<template>
  <div class="session-list">
    <button class="new-chat-btn" @click="$emit('newSession')">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
      </svg>
      新对话
    </button>
    <div class="sessions">
      <div
        v-for="session in sessions" :key="session.id"
        :class="['session-item', { active: session.id === currentId }]"
        @click="$emit('switch', session.id)"
      >
        <div class="session-info">
          <span class="session-title">{{ session.title || '新对话' }}</span>
          <span class="session-time">{{ session.messageCount }} 条消息</span>
        </div>
        <button class="session-delete" @click.stop="$emit('delete', session.id)" title="删除">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/>
          </svg>
        </button>
      </div>
      <div v-if="sessions.length === 0" class="no-sessions">暂无对话</div>
    </div>
  </div>
</template>

<script setup>
defineProps({ sessions: Array, currentId: String })
defineEmits(['newSession', 'switch', 'delete'])
</script>

<style scoped>
.session-list { padding: 0 12px 12px; display: flex; flex-direction: column; height: 100%; }
.new-chat-btn {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  width: 100%; padding: 10px 0; border-radius: var(--radius-sm);
  font-size: 13px; font-weight: 500; cursor: pointer;
  background: transparent; color: var(--accent);
  border: 1px solid var(--border);
  font-family: inherit; transition: all 0.2s ease;
  flex-shrink: 0;
}
.new-chat-btn:hover { background: var(--accent-soft); border-color: rgba(34,197,94,0.3); }
.sessions { flex: 1; overflow-y: auto; margin-top: 8px; }
.session-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 12px; border-radius: var(--radius-sm); cursor: pointer;
  margin-bottom: 2px; transition: background 0.15s ease;
}
.session-item:hover { background: var(--bg-hover); }
.session-item.active { background: var(--bg-card); }
.session-info { display: flex; flex-direction: column; gap: 2px; overflow: hidden; flex: 1; }
.session-title {
  font-size: 13.5px; color: var(--text-primary); font-weight: 500;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  line-height: 1.4;
}
.session-item.active .session-title { color: var(--text-primary); }
.session-time { font-size: 11.5px; color: var(--text-muted); }
.session-delete {
  background: none; border: none; cursor: pointer; color: var(--text-muted);
  padding: 4px; border-radius: 4px; display: flex; opacity: 0;
  transition: opacity 0.15s ease;
}
.session-item:hover .session-delete { opacity: 1; }
.session-delete:hover { color: #EF4444; background: rgba(239,68,68,0.1); }
.no-sessions { text-align: center; color: var(--text-muted); font-size: 13px; padding: 24px 0; }
</style>
