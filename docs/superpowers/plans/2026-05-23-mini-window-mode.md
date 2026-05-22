# 小窗模式实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 Vite + Vue 3 项目上叠加 Electron，实现桌面悬浮小窗，用户可在使用其他应用时始终置顶进行 AI 对话。

**Architecture:** Electron 主进程管理两个 BrowserWindow（完整窗口 + 小窗），加载同一 Vue 应用。通过 URL 参数 `?mini=1` 区分布局，IPC 在窗口间转发同步操作。完整窗口使用 HomeView（侧边栏+顶栏+聊天），小窗使用 MiniView（仅聊天+输入）。

**Tech Stack:** Electron 33, electron-builder, Vite 5, Vue 3, Pinia

---

### Task 1: 添加 Electron 依赖和脚本

**Files:**
- Modify: `frontend/package.json`

- [ ] **Step 1: 添加 electron 和 electron-builder 依赖**

在 `devDependencies` 中添加 electron 和 electron-builder：

```json
{
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.2.0",
    "electron": "^33.0.0",
    "electron-builder": "^25.0.0"
  }
}
```

在 `scripts` 中添加 Electron 开发和打包命令，并添加 `"main"` 字段指向 Electron 入口：

```json
{
  "main": "electron/main.js",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "electron:dev": "vite build && electron .",
    "electron:build": "vite build && electron-builder --win"
  }
}
```

添加 electron-builder 配置：

```json
{
  "build": {
    "appId": "com.voiceassistant.app",
    "productName": "AI 助手",
    "directories": {
      "output": "dist-electron"
    },
    "files": [
      "dist/**/*",
      "electron/**/*"
    ],
    "win": {
      "target": "nsis",
      "icon": null
    },
    "nsis": {
      "oneClick": false,
      "allowToChangeInstallationDirectory": true
    }
  }
}
```

- [ ] **Step 2: 安装依赖并验证**

```bash
cd frontend && npm install
```

Expected: 安装成功，无报错。

- [ ] **Step 3: Commit**

```bash
git add frontend/package.json frontend/package-lock.json
git commit -m "feat: add Electron dependencies and scripts for mini window mode"
```

---

### Task 2: 更新 Vite 配置适配 Electron

**Files:**
- Modify: `frontend/vite.config.js`

- [ ] **Step 1: 添加 base 配置确保 Electron 加载本地文件路径正确**

将 `frontend/vite.config.js` 修改为：

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  base: './',   // Electron 加载本地文件需要相对路径
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist'
  }
})
```

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npm run build
```

Expected: 构建成功，`dist/index.html` 中的资源路径为相对路径（`./assets/...` 而非 `/assets/...`）。

- [ ] **Step 3: Commit**

```bash
git add frontend/vite.config.js
git commit -m "feat: update Vite config for Electron (relative base path)"
```

---

### Task 3: 创建 Electron 主进程

**Files:**
- Create: `frontend/electron/main.js`

- [ ] **Step 1: 编写 main.js**

```javascript
const { app, BrowserWindow, ipcMain, screen } = require('electron')
const path = require('path')

let mainWindow = null
let miniWindow = null

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  // 生产环境加载打包后的文件，开发环境加载 Vite dev server
  const isDev = !app.isPackaged
  if (isDev) {
    mainWindow.loadURL('http://localhost:5173')
  } else {
    mainWindow.loadFile(path.join(__dirname, '..', 'dist', 'index.html'))
  }

  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

function createMiniWindow(sessionId) {
  // 如果小窗已存在，直接聚焦
  if (miniWindow && !miniWindow.isDestroyed()) {
    miniWindow.focus()
    if (sessionId) {
      miniWindow.webContents.send('sync-action', { type: 'session-switched', sessionId })
    }
    return
  }

  const { width: screenWidth, height: screenHeight } = screen.getPrimaryDisplay().workAreaSize
  const winWidth = 420
  const winHeight = 620

  miniWindow = new BrowserWindow({
    width: winWidth,
    height: winHeight,
    x: screenWidth - winWidth - 20,
    y: screenHeight - winHeight - 20,
    minWidth: 320,
    minHeight: 400,
    frame: false,
    alwaysOnTop: true,
    resizable: true,
    skipTaskbar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  const isDev = !app.isPackaged
  const baseUrl = isDev ? 'http://localhost:5173' : `file://${path.join(__dirname, '..', 'dist', 'index.html')}`
  miniWindow.loadURL(`${baseUrl}?mini=1`)

  // 小窗加载完成后同步会话
  if (sessionId) {
    miniWindow.webContents.once('did-finish-load', () => {
      miniWindow.webContents.send('sync-action', { type: 'session-switched', sessionId })
    })
  }

  miniWindow.on('closed', () => {
    miniWindow = null
    // 通知完整窗口小窗已关闭
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('mini-closed')
    }
  })
}

// IPC 处理
ipcMain.handle('open-mini', (event, sessionId) => {
  createMiniWindow(sessionId)
  return true
})

ipcMain.handle('close-mini', () => {
  if (miniWindow && !miniWindow.isDestroyed()) {
    miniWindow.close()
  }
  return true
})

ipcMain.on('sync-action', (event, action) => {
  // 转发同步操作到另一个窗口
  const sender = event.sender
  if (sender === mainWindow?.webContents && miniWindow && !miniWindow.isDestroyed()) {
    miniWindow.webContents.send('sync-action', action)
  } else if (sender === miniWindow?.webContents && mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send('sync-action', action)
  }
})

// 应用就绪
app.whenReady().then(() => {
  createMainWindow()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createMainWindow()
    }
  })
})

// 所有窗口关闭时退出
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('before-quit', () => {
  if (miniWindow && !miniWindow.isDestroyed()) {
    miniWindow.close()
  }
})
```

- [ ] **Step 2: 验证 Electron 主进程语法**

```bash
cd frontend && node -c electron/main.js
```

Expected: 无语法错误。

- [ ] **Step 3: Commit**

```bash
git add frontend/electron/main.js
git commit -m "feat: add Electron main process with dual window management"
```

---

### Task 4: 创建预加载脚本

**Files:**
- Create: `frontend/electron/preload.js`

- [ ] **Step 1: 编写 preload.js**

```javascript
const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('electronAPI', {
  // 打开小窗
  openMini: (sessionId) => ipcRenderer.invoke('open-mini', sessionId),

  // 关闭小窗
  closeMini: () => ipcRenderer.invoke('close-mini'),

  // 当前是否为小窗
  isMini: window.location.search.includes('mini=1'),

  // 监听来自另一窗口的同步操作
  onSyncAction: (callback) => {
    ipcRenderer.on('sync-action', (_event, action) => callback(action))
  },

  // 监听小窗关闭
  onMiniClosed: (callback) => {
    ipcRenderer.on('mini-closed', () => callback())
  },

  // 发送同步操作到另一窗口
  sendSyncAction: (action) => ipcRenderer.send('sync-action', action)
})
```

- [ ] **Step 2: 验证语法**

```bash
cd frontend && node -c electron/preload.js
```

Expected: 无语法错误。

- [ ] **Step 3: Commit**

```bash
git add frontend/electron/preload.js
git commit -m "feat: add Electron preload script with IPC bridge"
```

---

### Task 5: 创建小窗紧凑布局 MiniView.vue

**Files:**
- Create: `frontend/src/views/MiniView.vue`

- [ ] **Step 1: 编写 MiniView.vue**

```vue
<template>
  <div class="mini-window">
    <!-- 自定义标题栏（可拖拽） -->
    <header class="mini-titlebar" @dblclick="toggleMaximize">
      <span class="mini-brand">◈ AI 助手</span>
      <div class="mini-actions">
        <button class="mini-btn" @click="restoreMain" title="返回完整窗口">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M15 3h6v6"/><path d="M10 14L21 3"/><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
          </svg>
        </button>
        <button class="mini-btn" @click="closeMini" title="关闭小窗">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
    </header>

    <!-- 消息区域 -->
    <div class="mini-chat">
      <div v-if="chatStore.messages.length === 0" class="mini-empty">
        <p class="mini-empty-title">AI 助手</p>
        <p class="mini-empty-desc">输入问题开始对话</p>
      </div>
      <div class="mini-messages" v-else>
        <div v-for="(msg, idx) in chatStore.messages" :key="idx" class="mini-msg">
          <div :class="['mini-bubble', msg.role === 'USER' ? 'user' : 'ai']" v-html="renderContent(msg.content)" />
        </div>
        <div ref="bottomRef" />
      </div>
    </div>

    <!-- 输入区域 -->
    <footer class="mini-input-area">
      <div class="mini-input-row">
        <VoiceButton
          :isSupported="speech.isSupported.value"
          :isListening="speech.isListening.value"
          :isPTT="speech.mode.value === 'ptt'"
          @start="speech.startListening()"
          @stop="speech.stopListening()"
        />
        <input
          v-model="textInput"
          class="mini-text-input"
          placeholder="输入问题…"
          @keydown.enter="sendText"
          :disabled="chatStore.isStreaming"
        />
        <button
          class="mini-send-btn"
          @click="sendText"
          :disabled="!textInput.trim() || chatStore.isStreaming"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
          </svg>
        </button>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { useChatStore } from '../stores/chatStore'
import { useConfigStore } from '../stores/configStore'
import { useSpeech } from '../composables/useSpeech'
import { safeMarkdown } from '../marked-setup'
import VoiceButton from '../components/VoiceButton.vue'

const chatStore = useChatStore()
const configStore = useConfigStore()
const speech = useSpeech()

const textInput = ref('')
const bottomRef = ref(null)

onMounted(async () => {
  await configStore.loadProviders()
  await chatStore.loadSessions()
  // IPC 同步由 chatStore 在模块初始化时自动注册，此处无需重复监听
})

// 监听流式内容变化时滚动到底部
watch(() => {
  const msgs = chatStore.messages
  return msgs && msgs.length ? msgs[msgs.length - 1].content : ''
}, () => {
  nextTick(() => bottomRef.value?.scrollIntoView({ behavior: 'smooth' }))
})

// 监听语音识别结果
watch(() => speech.transcript.value, (val) => {
  if (val) textInput.value = val
})

function renderContent(content) {
  if (!content) return '<span style="color: var(--text-muted); font-style: italic;">思考中…</span>'
  return safeMarkdown(content)
}

function sendText() {
  const content = textInput.value.trim()
  if (!content) return
  textInput.value = ''
  chatStore.sendMessage(content, configStore.chatMode, configStore.activeProviderId)
}

function restoreMain() {
  // 聚焦完整窗口
  window.electronAPI?.closeMini()
}

function closeMini() {
  window.electronAPI?.closeMini()
}
</script>

<style scoped>
.mini-window {
  display: flex; flex-direction: column;
  height: 100vh; background: var(--bg-primary);
  user-select: none;
}

/* 标题栏 */
.mini-titlebar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 10px; height: 32px; flex-shrink: 0;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border);
  -webkit-app-region: drag;
}
.mini-brand { font-size: 12px; font-weight: 600; color: var(--text-secondary); }
.mini-actions { display: flex; gap: 4px; -webkit-app-region: no-drag; }
.mini-btn {
  width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;
  background: transparent; border: none; color: var(--text-muted); cursor: pointer;
  border-radius: 4px; transition: all 0.15s ease;
}
.mini-btn:hover { background: var(--bg-hover); color: var(--text-primary); }

/* 聊天区域 */
.mini-chat {
  flex: 1; overflow-y: auto; padding: 12px;
}
.mini-empty { text-align: center; padding-top: 40%; opacity: 0.6; }
.mini-empty-title { font-size: 15px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.mini-empty-desc { font-size: 12px; color: var(--text-muted); }
.mini-msg { margin-bottom: 14px; }
.mini-bubble {
  max-width: 95%; padding: 10px 14px; border-radius: var(--radius-md);
  font-size: 13px; line-height: 1.65; word-break: break-word;
}
.mini-bubble.user {
  background: var(--user-bubble); color: var(--user-bubble-text);
  margin-left: auto; border-bottom-right-radius: 4px;
}
.mini-bubble.ai {
  background: var(--ai-bubble); color: var(--ai-bubble-text);
  margin-right: auto; border-bottom-left-radius: 4px;
  border: 1px solid var(--border);
}

/* Markdown 精简样式 */
.mini-bubble :deep(h1) { font-size: 1.2em; margin: 12px 0 8px; }
.mini-bubble :deep(h2) { font-size: 1.1em; margin: 10px 0 6px; }
.mini-bubble :deep(h3) { font-size: 1.05em; margin: 8px 0 4px; }
.mini-bubble :deep(p) { margin: 0 0 8px 0; }
.mini-bubble :deep(p:last-child) { margin-bottom: 0; }
.mini-bubble :deep(ul), .mini-bubble :deep(ol) { padding-left: 18px; margin: 6px 0; }
.mini-bubble :deep(code) {
  font-size: 0.85em; background: rgba(255,255,255,0.08); color: #e879f9;
  padding: 1px 5px; border-radius: 3px;
}
.mini-bubble :deep(pre) {
  margin: 8px 0; padding: 10px 12px; border-radius: var(--radius-sm);
  background: #0D1520; border: 1px solid var(--border);
  font-size: 12px; overflow-x: auto;
}
.mini-bubble :deep(blockquote) {
  border-left: 2px solid var(--accent); margin: 8px 0; padding: 6px 10px;
  background: rgba(34,197,94,0.05); border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  color: var(--text-secondary); font-size: 12px;
}
.mini-bubble :deep(a) { color: var(--accent); }

/* 输入区域 */
.mini-input-area { padding: 8px 10px 10px; flex-shrink: 0; border-top: 1px solid var(--border); background: var(--bg-secondary); }
.mini-input-row { display: flex; align-items: center; gap: 8px; }
.mini-text-input {
  flex: 1; padding: 8px 12px; background: var(--bg-input);
  border: 1px solid var(--border); border-radius: var(--radius-sm);
  color: var(--text-primary); font-size: 13px; font-family: inherit;
  outline: none; transition: border-color 0.2s ease;
}
.mini-text-input:focus { border-color: var(--accent); }
.mini-text-input::placeholder { color: var(--text-muted); }
.mini-text-input:disabled { opacity: 0.5; }
.mini-send-btn {
  width: 36px; height: 36px; flex-shrink: 0; display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-sm); border: none; cursor: pointer; transition: all 0.2s ease;
  background: var(--bg-input); color: var(--text-muted);
}
.mini-send-btn:not(:disabled) { background: var(--accent); color: #fff; }
.mini-send-btn:not(:disabled):hover { background: var(--accent-hover); }
.mini-send-btn:disabled { cursor: not-allowed; opacity: 0.4; }
</style>
```

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npm run build
```

Expected: 构建成功无报错。

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/MiniView.vue
git commit -m "feat: add MiniView compact layout for mini window mode"
```

---

### Task 6: 更新 App.vue 布局路由

**Files:**
- Modify: `frontend/src/App.vue`

- [ ] **Step 1: 根据 URL 参数切换布局**

将 `App.vue` 的 `<script setup>` 部分修改为：

```vue
<template>
  <MiniView v-if="isMini" />
  <HomeView v-else />
</template>

<script setup>
import { ref } from 'vue'
import HomeView from './views/HomeView.vue'
import MiniView from './views/MiniView.vue'

// URL 参数 ?mini=1 判断是否为小窗模式
const isMini = ref(window.location.search.includes('mini=1'))
</script>
```

`<style>` 部分保持不变。

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npm run build
```

Expected: 构建成功无报错。

- [ ] **Step 3: Commit**

```bash
git add frontend/src/App.vue
git commit -m "feat: add layout routing in App.vue for mini window mode"
```

---

### Task 7: 在 HomeView 添加"弹出小窗"按钮

**Files:**
- Modify: `frontend/src/views/HomeView.vue`

- [ ] **Step 1: 在顶栏右侧添加弹出小窗按钮**

在 `topbar-right` div 中添加按钮，放在 mode-toggle 之前：

```vue
<template>
  <!-- 其余模板不变，在 topbar-right 中添加 -->
  <div class="topbar-right">
    <el-button
      v-if="typeof window !== 'undefined' && window.electronAPI"
      size="small" text
      @click="openMiniWindow"
      class="mini-toggle"
      title="弹出小窗"
    >
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M15 3h6v6"/><path d="M10 14L21 3"/><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
      </svg>
    </el-button>
    <el-button size="small" text @click="toggleSpeechMode" class="mode-toggle">
      {{ speech.mode.value === 'ptt' ? '实时模式' : '按键模式' }}
    </el-button>
    <el-button size="small" text :icon="Setting" @click="showProviderDialog = true" class="settings-btn" />
  </div>
</template>
```

在 `<script setup>` 中添加 `openMiniWindow` 函数：

```javascript
function openMiniWindow() {
  window.electronAPI?.openMini(chatStore.currentSessionId)
}
```

在 `<style scoped>` 中添加按钮样式：

```css
.mini-toggle { color: var(--text-muted) !important; font-size: 13px !important; }
.mini-toggle:hover { color: var(--accent) !important; }
```

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npm run build
```

Expected: 构建成功无报错。

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/HomeView.vue
git commit -m "feat: add pop-out mini window button to HomeView topbar"
```

---

### Task 8: 在 chatStore 添加 IPC 同步钩子

**Files:**
- Modify: `frontend/src/stores/chatStore.js`

- [ ] **Step 1: 在关键操作中添加同步广播**

在 `chatStore.js` 的 `sendMessage` 函数 `onDone` 回调中添加同步调用。同时为 `switchSession` 和 `deleteCurrentSession` 添加同步。

修改后的完整 `chatStore.js`：

```javascript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as api from '../services/api'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref([])
  const currentSessionId = ref(null)
  const messages = ref([])
  const isStreaming = ref(false)
  const streamingContent = ref('')

  const currentSession = computed(() =>
    sessions.value.find(s => s.id === currentSessionId.value)
  )

  // 发送同步操作到另一窗口（Electron IPC）
  function broadcastSync(type, extra = {}) {
    window.electronAPI?.sendSyncAction({ type, sessionId: currentSessionId.value, ...extra })
  }

  // 处理来自另一窗口的同步操作
  async function handleSyncAction(action) {
    switch (action.type) {
      case 'message-sent':
        if (currentSessionId.value === action.sessionId) {
          messages.value = await api.getSessionMessages(action.sessionId)
        }
        break
      case 'session-switched':
        if (currentSessionId.value !== action.sessionId && action.sessionId) {
          await switchSession(action.sessionId)
        }
        break
      case 'session-deleted':
        await loadSessions()
        break
    }
  }

  // 注册 IPC 同步监听（Electron 环境）
  if (typeof window !== 'undefined' && window.electronAPI) {
    window.electronAPI.onSyncAction(handleSyncAction)
  }

  async function loadSessions() {
    sessions.value = await api.listSessions()
  }

  async function createNewSession() {
    const session = await api.createSession()
    sessions.value.unshift(session)
    switchSession(session.id)
    broadcastSync('session-created', { newSessionId: session.id })
    return session
  }

  async function switchSession(sessionId) {
    currentSessionId.value = sessionId
    messages.value = await api.getSessionMessages(sessionId)
    broadcastSync('session-switched', { sessionId })
  }

  async function deleteCurrentSession(id) {
    await api.deleteSession(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    if (currentSessionId.value === id) {
      currentSessionId.value = null
      messages.value = []
    }
    broadcastSync('session-deleted')
  }

  async function sendMessage(content, mode, providerId) {
    if (!currentSessionId.value) {
      await createNewSession()
    }

    const userMsg = { role: 'USER', content, timestamp: new Date().toISOString() }
    messages.value = [...messages.value, userMsg]

    const assistantMsg = { role: 'ASSISTANT', content: '', timestamp: new Date().toISOString(), streaming: true }
    messages.value = [...messages.value, assistantMsg]

    isStreaming.value = true
    streamingContent.value = ''
    const aiIndex = messages.value.length - 1

    await api.sendMessageStream(
      { sessionId: currentSessionId.value, content, mode, providerId },
      {
        onToken(token) {
          streamingContent.value += token
          const arr = [...messages.value]
          arr[aiIndex] = { ...arr[aiIndex], content: streamingContent.value }
          messages.value = arr
        },
        onDone() {
          const arr = [...messages.value]
          const item = { ...arr[aiIndex] }
          delete item.streaming
          arr[aiIndex] = item
          messages.value = arr
          isStreaming.value = false
          streamingContent.value = ''
          loadSessions()
          // IPC 同步：通知另一窗口刷新消息
          broadcastSync('message-sent')
        },
        onError(err) {
          console.error('Stream error:', err)
          const arr = [...messages.value]
          arr[aiIndex] = { ...arr[aiIndex], content: '请求失败: ' + err.message, streaming: undefined }
          messages.value = arr
          isStreaming.value = false
        }
      }
    )
  }

  return {
    sessions, currentSessionId, messages, isStreaming, streamingContent,
    currentSession, loadSessions, createNewSession, switchSession,
    deleteCurrentSession, sendMessage, handleSyncAction
  }
})
```

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npm run build
```

Expected: 构建成功无报错。

- [ ] **Step 3: Commit**

```bash
git add frontend/src/stores/chatStore.js
git commit -m "feat: add IPC sync hooks in chatStore for cross-window state sync"
```

---

### Task 9: 端到端验证

**Files:** 无（手动验证）

- [ ] **Step 1: 构建前端**

```bash
cd frontend && npm run build
```

Expected: 构建成功。

- [ ] **Step 2: 启动 Electron 完整窗口**

```bash
cd frontend && npm run electron:dev
```

Expected: Electron 启动，显示完整窗口（1280×800），侧边栏、顶栏、聊天区均正常。

- [ ] **Step 3: 点击"弹出小窗"按钮**

点击顶栏右侧的弹出图标。

Expected: 屏幕右下角弹出无边框小窗（420×620），始终置顶。小窗显示紧凑聊天布局。

- [ ] **Step 4: 验证消息同步**

在完整窗口发送消息，等待回复完成后 → 切换到小窗，查看消息是否一致。反之亦然。

Expected: 两边消息保持同步。

- [ ] **Step 5: 验证窗口交互**

- 拖拽小窗标题栏 → 窗口移动
- 关闭小窗 → 完整窗口仍然运行
- 关闭完整窗口 → 小窗随之关闭

Expected: 所有交互符合预期。
