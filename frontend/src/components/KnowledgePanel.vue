<template>
  <div class="knowledge-panel">
    <div class="panel-header">
      <span class="panel-title">知识库</span>
      <el-upload
        :show-file-list="false"
        :before-upload="handleUpload"
        accept=".txt,.md,.pdf"
      >
        <el-button :icon="Upload" size="small" type="primary" :loading="uploading">
          上传文档
        </el-button>
      </el-upload>
    </div>

    <div class="doc-list">
      <div v-if="documents.length === 0" class="empty-hint">
        暂无文档，上传 TXT、Markdown 或 PDF 文件
      </div>
      <div v-for="doc in documents" :key="doc.id" class="doc-item">
        <div class="doc-info">
          <span class="doc-name">{{ doc.fileName }}</span>
          <span class="doc-meta">{{ doc.chunkCount }} 块 · {{ formatSize(doc.fileSize) }}</span>
        </div>
        <el-button
          :icon="Delete"
          circle
          size="small"
          text
          @click="handleDelete(doc.id)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Upload, Delete } from '@element-plus/icons-vue'
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
  return false // prevent default upload
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定要删除该文档吗？所有相关的向量数据也将被删除。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.deleteDocument(id)
    ElMessage.success('文档已删除')
    await loadDocuments()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败: ' + e.message)
  }
}

function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}
</script>

<style scoped>
.knowledge-panel { padding: 12px; }
.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.panel-title { font-size: 15px; font-weight: 600; color: #303133; }
.doc-list { max-height: calc(100vh - 140px); overflow-y: auto; }
.empty-hint { color: #909399; font-size: 13px; text-align: center; margin-top: 24px; }
.doc-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 12px; border-radius: 6px; margin-bottom: 4px;
}
.doc-item:hover { background: #f0f2f5; }
.doc-info { display: flex; flex-direction: column; gap: 2px; overflow: hidden; }
.doc-name { font-size: 14px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.doc-meta { font-size: 12px; color: #909399; }
</style>
