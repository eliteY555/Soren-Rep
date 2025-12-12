<template>
  <div class="employee-container">
    <el-card class="header-card">
      <div class="header-content">
        <div>
          <h2>员工管理</h2>
          <p class="subtitle">管理您的员工账号</p>
        </div>
        <el-button type="primary" @click="showRegisterDialog = true" :icon="Plus">
          注册员工账号
        </el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table 
        :data="employeeList" 
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa' }"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="idCard" label="身份证号" width="180" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.gender === 1" type="primary" size="small">男</el-tag>
            <el-tag v-else-if="row.gender === 0" type="warning" size="small">女</el-tag>
            <el-tag v-else type="info" size="small">未知</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small">正常</el-tag>
            <el-tag v-else type="danger" size="small">已冻结</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="280">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
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
          @size-change="loadEmployees"
          @current-change="loadEmployees"
        />
      </div>
    </el-card>

    <!-- 注册员工对话框 -->
    <el-dialog
      v-model="showRegisterDialog"
      title="注册员工"
      width="500px"
      @close="resetRegisterForm"
    >
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-width="100px"
      >
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="registerForm.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="registerForm.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showRegisterDialog = false">取消</el-button>
        <el-button type="primary" @click="handleRegister" :loading="registerLoading">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 注册成功对话框 -->
    <el-dialog
      v-model="showSuccessDialog"
      title="注册成功"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-result icon="success" title="员工注册成功！" sub-title="请将以下信息告知员工">
        <template #extra>
          <div class="success-info">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="员工姓名">
                {{ registerResult?.realName }}
              </el-descriptions-item>
              <el-descriptions-item label="登录账号（手机号）">
                <span class="highlight">{{ registerResult?.phone }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="默认密码">
                <span class="highlight">{{ registerResult?.defaultPassword }}</span>
              </el-descriptions-item>
            </el-descriptions>
            <el-alert
              class="mt-20"
              title="提示：请员工首次登录后及时修改密码"
              type="warning"
              :closable="false"
            />
          </div>
        </template>
      </el-result>

      <template #footer>
        <el-button type="primary" @click="showSuccessDialog = false">我知道了</el-button>
      </template>
    </el-dialog>

    <!-- 员工详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="员工详情"
      width="600px"
    >
      <el-descriptions :column="2" border v-if="currentEmployee">
        <el-descriptions-item label="ID">{{ currentEmployee.id }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ currentEmployee.realName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentEmployee.phone }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ currentEmployee.idCard }}</el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ currentEmployee.gender === 1 ? '男' : currentEmployee.gender === 0 ? '女' : '未知' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentEmployee.status === 1" type="success">正常</el-tag>
          <el-tag v-else type="danger">已冻结</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">
          {{ currentEmployee.createdAt }}
        </el-descriptions-item>
        <el-descriptions-item label="最后登录" :span="2">
          {{ currentEmployee.lastLoginTime || '从未登录' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  registerEmployee,
  getMyEmployees,
  freezeEmployee,
  unfreezeEmployee,
  resetPassword,
  Employee,
  EmployeeRegisterVO
} from '@/api/employee'

const loading = ref(false)
const registerLoading = ref(false)
const showRegisterDialog = ref(false)
const showSuccessDialog = ref(false)
const showDetailDialog = ref(false)

const employeeList = ref<Employee[]>([])
const currentEmployee = ref<Employee | null>(null)
const registerResult = ref<EmployeeRegisterVO | null>(null)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const registerFormRef = ref<FormInstance>()
const registerForm = reactive({
  realName: '',
  phone: '',
  idCard: '',
  gender: 1
})

const registerRules: FormRules = {
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
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ]
}

// 加载员工列表
const loadEmployees = async () => {
  loading.value = true
  try {
    const res: any = await getMyEmployees(pagination.pageNum, pagination.pageSize)
    employeeList.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('加载员工列表失败:', error)
    ElMessage.error('加载员工列表失败')
  } finally {
    loading.value = false
  }
}

// 注册员工
const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return

    registerLoading.value = true
    try {
      const res: any = await registerEmployee(registerForm)
      registerResult.value = res.data
      showRegisterDialog.value = false
      showSuccessDialog.value = true
      await loadEmployees()
    } catch (error) {
      console.error('注册员工失败:', error)
    } finally {
      registerLoading.value = false
    }
  })
}

// 冻结/解冻员工
const handleToggleStatus = async (row: Employee) => {
  const action = row.status === 1 ? '冻结' : '解冻'
  try {
    await ElMessageBox.confirm(
      `确定要${action}员工 ${row.realName} 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (row.status === 1) {
      await freezeEmployee(row.id)
    } else {
      await unfreezeEmployee(row.id)
    }

    ElMessage.success(`${action}成功`)
    await loadEmployees()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}员工失败:`, error)
    }
  }
}

// 重置密码
const handleResetPassword = async (row: Employee) => {
  try {
    await ElMessageBox.confirm(
      `确定要重置员工 ${row.realName} 的密码吗？密码将重置为：123456`,
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

// 查看详情
const handleDetail = (row: Employee) => {
  currentEmployee.value = row
  showDetailDialog.value = true
}

// 重置注册表单
const resetRegisterForm = () => {
  registerFormRef.value?.resetFields()
  Object.assign(registerForm, {
    realName: '',
    phone: '',
    idCard: '',
    gender: 1
  })
}

onMounted(() => {
  loadEmployees()
})
</script>

<style scoped lang="scss">
.employee-container {
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

.success-info {
  padding: 20px;

  .highlight {
    color: #409eff;
    font-weight: bold;
    font-size: 16px;
  }

  .mt-20 {
    margin-top: 20px;
  }
}
</style>

