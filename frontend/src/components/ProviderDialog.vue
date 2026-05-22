<template>
  <el-dialog v-model="visible" title="模型提供商管理" width="560px" @close="$emit('close')">
    <!-- Existing providers list -->
    <div class="provider-list">
      <div v-for="p in configStore.providers" :key="p.id" class="provider-card">
        <div class="provider-info">
          <el-tag :type="p.active ? 'success' : 'info'" size="small" effect="dark">
            {{ p.active ? '当前' : p.provider }}
          </el-tag>
          <span class="provider-name">{{ p.name }}</span>
          <span class="provider-model">{{ p.modelName }}</span>
        </div>
        <div class="provider-actions">
          <el-button size="small" text @click="editProvider(p)" v-if="!editing || editing.id !== p.id">
            编辑
          </el-button>
          <el-button v-if="!p.active" size="small" text type="danger" @click="handleDelete(p.id)">
            删除
          </el-button>
          <el-button v-if="!p.active" size="small" text type="primary" @click="handleActivate(p.id)">
            启用
          </el-button>
        </div>
      </div>
    </div>

    <el-divider />

    <!-- Add / Edit form -->
    <div class="form-title">{{ editing ? '编辑 ' + editing.name : '添加新提供商' }}</div>
    <el-form :model="form" label-position="top" size="small">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="显示名称">
            <el-input v-model="form.name" placeholder="如 DeepSeek V4 Pro" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="提供商标识">
            <el-select v-model="form.provider" placeholder="选择类型">
              <el-option label="OpenAI 兼容" value="openai" />
              <el-option label="DeepSeek" value="deepseek" />
              <el-option label="通义千问" value="qwen" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="Base URL">
        <el-input v-model="form.baseUrl" placeholder="https://api.deepseek.com/v1" />
      </el-form-item>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="模型名称">
            <el-input v-model="form.modelName" placeholder="deepseek-chat" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="API Key">
            <el-input v-model="form.apiKey" type="password" show-password placeholder="sk-xxx" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        {{ editing ? '保存修改' : '添加' }}
      </el-button>
      <el-button v-if="editing" @click="cancelEdit">取消</el-button>
    </el-form>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useConfigStore } from '../stores/configStore'

const emit = defineEmits(['close'])
const configStore = useConfigStore()

const visible = ref(true)
const submitting = ref(false)
const editing = ref(null)

const emptyForm = () => ({
  name: '',
  provider: 'deepseek',
  baseUrl: '',
  modelName: '',
  apiKey: ''
})

const form = reactive(emptyForm())

function editProvider(p) {
  editing.value = p
  form.name = p.name
  form.provider = p.provider
  form.baseUrl = p.baseUrl || ''
  form.modelName = p.modelName
  form.apiKey = ''
}

function cancelEdit() {
  editing.value = null
  Object.assign(form, emptyForm())
}

async function handleSubmit() {
  if (!form.name || !form.modelName) {
    ElMessage.warning('请填写名称和模型名称')
    return
  }
  submitting.value = true
  try {
    if (editing.value) {
      await configStore.updateProvider(editing.value.id, { ...form })
      ElMessage.success('提供商已更新')
      cancelEdit()
    } else {
      if (!form.apiKey) { ElMessage.warning('请填写 API Key'); submitting.value = false; return }
      await configStore.addProvider({ ...form })
      ElMessage.success('提供商已添加')
      Object.assign(form, emptyForm())
    }
  } catch (e) {
    ElMessage.error('操作失败: ' + e.message)
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该提供商配置？', '确认', { type: 'warning' })
    await configStore.deleteProvider(id)
    ElMessage.success('已删除')
  } catch (e) { /* cancelled */ }
}

async function handleActivate(id) {
  await configStore.activateProvider(id)
  ElMessage.success('已切换')
}
</script>

<style scoped>
.provider-list { max-height: 200px; overflow-y: auto; }
.provider-card {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 0; border-bottom: 1px solid #f0f0f0;
}
.provider-info { display: flex; align-items: center; gap: 10px; }
.provider-name { font-weight: 500; font-size: 14px; }
.provider-model { color: #909399; font-size: 13px; }
.provider-actions { display: flex; gap: 4px; }
.form-title { font-weight: 600; margin-bottom: 12px; font-size: 14px; }
</style>
