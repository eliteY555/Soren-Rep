<template>
  <MiniView v-if="isMini" />
  <HomeView v-else />
</template>

<script setup>
import { ref } from 'vue'
import HomeView from './views/HomeView.vue'
import MiniView from './views/MiniView.vue'

const isMini = ref(window.location.search.includes('mini=1'))
</script>

<style>
:root {
  --bg-primary: #0B1120;
  --bg-secondary: #111827;
  --bg-card: #1A2332;
  --bg-hover: #1E293B;
  --bg-input: #151D2B;
  --text-primary: #F1F5F9;
  --text-secondary: #94A3B8;
  --text-muted: #64748B;
  --accent: #22C55E;
  --accent-hover: #16A34A;
  --accent-soft: rgba(34, 197, 94, 0.12);
  --user-bubble: #22C55E;
  --user-bubble-text: #FFFFFF;
  --ai-bubble: #1E293B;
  --ai-bubble-text: #E2E8F0;
  --border: #1E293B;
  --border-light: #263348;
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.3);
  --shadow-md: 0 4px 12px rgba(0,0,0,0.4);
  --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --font-mono: 'Fira Code', 'Cascadia Code', 'JetBrains Mono', monospace;
}

* { margin: 0; padding: 0; box-sizing: border-box; }

body {
  font-family: var(--font-sans);
  background: var(--bg-primary);
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  line-height: 1.6;
}

#app { height: 100vh; overflow: hidden; }

/* Scrollbar */
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--border-light); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: var(--text-muted); }

/* Element Plus dark theme overrides */
:root {
  --el-color-primary: var(--accent);
  --el-color-primary-light-3: var(--accent-hover);
  --el-bg-color: var(--bg-secondary);
  --el-bg-color-overlay: var(--bg-card);
  --el-text-color-primary: var(--text-primary);
  --el-text-color-regular: var(--text-secondary);
  --el-border-color: var(--border);
  --el-border-color-light: var(--border-light);
  --el-fill-color: var(--bg-hover);
  --el-fill-color-blank: var(--bg-input);
  --el-input-bg-color: var(--bg-input);
  --el-input-text-color: var(--text-primary);
  --el-input-placeholder-color: var(--text-muted);
  --el-input-border-color: var(--border);
  --el-input-hover-border-color: var(--border-light);
  --el-input-focus-border-color: var(--accent);
  --el-dialog-bg-color: var(--bg-card);
  --el-mask-color: rgba(0,0,0,0.6);
}

body {
  background: var(--bg-primary);
}
</style>
