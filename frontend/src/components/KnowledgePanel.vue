<template>
  <div class="knowledge-panel">
    <div class="panel-header">
      <div class="header-left">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
        </svg>
        <span class="panel-title">知识库</span>
      </div>
      <el-upload
        :show-file-list="false"
        :before-upload="handleUpload"
        accept=".txt,.md,.pdf"
      >
        <el-button :icon="Upload" size="small" :loading="uploading">上传</el-button>
      </el-upload>
    </div>

    <div class="doc-list">
      <div v-if="documents.length === 0" class="empty-hint">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="color: var(--text-muted); margin-bottom: 12px;">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
        </svg>
        <p>暂无文档</p>
        <p class="sub-hint">上传 TXT、Markdown 或 PDF</p>
      </div>
      <div v-for="doc in documents" :key="doc.id" class="doc-item">
        <div class="doc-info">
          <span class="doc-name">{{ doc.fileName }}</span>
          <span class="doc-meta">{{ doc.chunkCount }} 个分块 · {{ formatSize(doc.fileSize) }}</span>
        </div>
        <button class="doc-delete" @click="handleDelete(doc.id)" title="删除">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '../services/api'

const documents = ref([])
const uploading = ref(false)
onMounted(() => { loadDocuments() })

async function loadDocuments() {
  documents.value = await api.listDocuments()
}
async function handleUpload(file) {
  uploading.value = true
  try {
    await api.uploadDocument(file)
    ElMessage.success(`"${file.name}" 上传成功`)
    await loadDocuments()
  } catch (e) {
    ElMessage.error('上传失败: ' + e.message)
  } finally {
    uploading.value = false
  }
  return false
}
async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该文档？向量数据将同步删除。', '确认删除', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await api.deleteDocument(id)
    ElMessage.success('已删除')
    await loadDocuments()
  } catch (e) { /* cancelled */ }
}
function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}
</script>

<style scoped>
.knowledge-panel { padding: 0 12px 12px; display: flex; flex-direction: column; height: 100%; }
.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px; padding: 4px 0; flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 8px; color: var(--text-secondary); }
.panel-title { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.doc-list { flex: 1; overflow-y: auto; }
.empty-hint {
  text-align: center; padding: 40px 0; color: var(--text-muted); font-size: 13px;
  display: flex; flex-direction: column; align-items: center;
}
.sub-hint { color: var(--text-muted); font-size: 12px; margin-top: 4px; opacity: 0.7; }
.doc-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 12px; border-radius: var(--radius-sm); margin-bottom: 2px;
  transition: background 0.15s ease;
}
.doc-item:hover { background: var(--bg-hover); }
.doc-info { display: flex; flex-direction: column; gap: 2px; overflow: hidden; }
.doc-name { font-size: 13px; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.doc-meta { font-size: 11.5px; color: var(--text-muted); }
.doc-delete {
  background: none; border: none; cursor: pointer; color: var(--text-muted);
  padding: 4px; border-radius: 4px; display: flex; opacity: 0;
  transition: opacity 0.15s ease;
}
.doc-item:hover .doc-delete { opacity: 1; }
.doc-delete:hover { color: #EF4444; background: rgba(239,68,68,0.1); }
</style>
