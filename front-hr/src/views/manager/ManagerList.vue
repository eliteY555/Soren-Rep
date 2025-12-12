<template>
  <div class="manager-container">
    <el-card class="header-card">
      <div class="header-content">
        <div>
          <h2>经理管理</h2>
          <p class="subtitle">创建和管理经理账号</p>
        </div>
        <el-button type="primary" @click="showCreateDialog = true" :icon="Plus">
          注册经理账号
        </el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table 
        :data="managerList" 
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa' }"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="idCard" label="身份证号" width="180" />
        <el-table-column prop="departmentId" label="部门ID" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small">正常</el-tag>
            <el-tag v-else type="danger" size="small">已冻结</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="280">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button 
              link 
              :type="row.status === 1 ? 'warning' : 'success'" 
              size="small" 
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '冻结' : '解冻' }}
            </el-button>
            <el-button link type="danger" size="small" @click="handleResetPassword(row)">
              重置密码
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadManagers"
          @current-change="loadManagers"
        />
      </div>
    </el-card>

    <!-- 创建经理对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建经理"
      width="500px"
      @close="resetCreateForm"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="100px"
      >
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="createForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="createForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="createForm.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="部门ID">
          <el-input-number v-model="createForm.departmentId" :min="1" placeholder="请输入部门ID" style="width: 100%" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="createLoading">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑经理对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑经理"
      width="500px"
      @close="resetEditForm"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="createRules"
        label-width="100px"
      >
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="editForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="部门ID">
          <el-input-number v-model="editForm.departmentId" :min="1" placeholder="请输入部门ID" style="width: 100%" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate" :loading="editLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  createManager,
  getManagerList,
  freezeManager,
  unfreezeManager,
  updateManager,
  resetPassword,
  Manager
} from '@/api/manager'

const loading = ref(false)
const createLoading = ref(false)
const editLoading = ref(false)
const showCreateDialog = ref(false)
const showEditDialog = ref(false)

const managerList = ref<Manager[]>([])
const currentManager = ref<Manager | null>(null)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const createFormRef = ref<FormInstance>()
const createForm = reactive({
  realName: '',
  phone: '',
  idCard: '',
  departmentId: undefined as number | undefined
})

const editFormRef = ref<FormInstance>()
const editForm = reactive({
  id: 0,
  realName: '',
  phone: '',
  departmentId: undefined as number | undefined
})

const createRules: FormRules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度在2-20个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号', trigger: 'blur' }
  ]
}

// 加载经理列表
const loadManagers = async () => {
  loading.value = true
  try {
    const res: any = await getManagerList(pagination.pageNum, pagination.pageSize)
    managerList.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('加载经理列表失败:', error)
    ElMessage.error('加载经理列表失败')
  } finally {
    loading.value = false
  }
}

// 创建经理
const handleCreate = async () => {
  if (!createFormRef.value) return

  await createFormRef.value.validate(async (valid) => {
    if (!valid) return

    createLoading.value = true
    try {
      await createManager(createForm)
      ElMessage.success('创建经理成功，默认密码：123456')
      showCreateDialog.value = false
      await loadManagers()
    } catch (error) {
      console.error('创建经理失败:', error)
    } finally {
      createLoading.value = false
    }
  })
}

// 编辑经理
const handleEdit = (row: Manager) => {
  currentManager.value = row
  editForm.id = row.id
  editForm.realName = row.realName
  editForm.phone = row.phone
  editForm.departmentId = row.departmentId
  showEditDialog.value = true
}

// 更新经理
const handleUpdate = async () => {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async (valid) => {
    if (!valid) return

    editLoading.value = true
    try {
      await updateManager(editForm.id, {
        realName: editForm.realName,
        phone: editForm.phone,
        departmentId: editForm.departmentId
      })
      ElMessage.success('更新成功')
      showEditDialog.value = false
      await loadManagers()
    } catch (error) {
      console.error('更新经理失败:', error)
    } finally {
      editLoading.value = false
    }
  })
}

// 冻结/解冻经理
const handleToggleStatus = async (row: Manager) => {
  const action = row.status === 1 ? '冻结' : '解冻'
  try {
    await ElMessageBox.confirm(
      `确定要${action}经理 ${row.realName} 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (row.status === 1) {
      await freezeManager(row.id)
    } else {
      await unfreezeManager(row.id)
    }

    ElMessage.success(`${action}成功`)
    await loadManagers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}经理失败:`, error)
    }
  }
}

// 重置密码
const handleResetPassword = async (row: Manager) => {
  try {
    await ElMessageBox.confirm(
      `确定要重置经理 ${row.realName} 的密码吗？密码将重置为：123456`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await resetPassword(row.id)
    ElMessage.success('密码重置成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置密码失败:', error)
    }
  }
}

// 重置创建表单
const resetCreateForm = () => {
  createFormRef.value?.resetFields()
  Object.assign(createForm, {
    realName: '',
    phone: '',
    idCard: '',
    departmentId: undefined
  })
}

// 重置编辑表单
const resetEditForm = () => {
  editFormRef.value?.resetFields()
  currentManager.value = null
}

onMounted(() => {
  loadManagers()
})
</script>

<style scoped lang="scss">
.manager-container {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;

  h2 {
    margin: 0 0 8px 0;
    font-size: 24px;
    color: #333;
  }

  .subtitle {
    margin: 0;
    color: #999;
    font-size: 14px;
  }
}

.table-card {
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>

