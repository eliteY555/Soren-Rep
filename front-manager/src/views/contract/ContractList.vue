<template>
  <div class="contract-container">
    <el-card class="header-card">
      <div class="header-content">
        <div>
          <h2>合同管理</h2>
          <p class="subtitle">管理您下发的员工劳动合同</p>
        </div>
        <el-button type="primary" @click="showIssueDialog = true" :icon="Plus">
          下发合同
        </el-button>
      </div>
    </el-card>

    <el-card class="filter-card">
      <el-radio-group v-model="statusFilter" @change="fetchContracts">
        <el-radio-button :value="undefined">全部</el-radio-button>
        <el-radio-button :value="1">待员工签字</el-radio-button>
        <el-radio-button :value="2">已生效</el-radio-button>
        <el-radio-button :value="3">已终止</el-radio-button>
      </el-radio-group>
    </el-card>

    <el-card class="table-card">
      <el-table 
        :data="contracts" 
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa' }"
      >
        <el-table-column prop="contractNo" label="合同编号" width="180" />
        <el-table-column prop="employeeName" label="员工姓名" width="120" />
        <el-table-column prop="position" label="岗位" width="150" />
        <el-table-column prop="department" label="部门" width="150" />
        <el-table-column prop="baseSalary" label="基本工资" width="120">
          <template #default="{ row }">
            {{ row.baseSalary ? `¥${row.baseSalary}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120">
          <template #default="{ row }">
            {{ row.endDate || '无固定期限' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="warning" size="small">待员工签字</el-tag>
            <el-tag v-else-if="row.status === 2" type="success" size="small">已生效</el-tag>
            <el-tag v-else-if="row.status === 3" type="info" size="small">已终止</el-tag>
            <el-tag v-else type="info" size="small">{{ row.statusDesc || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-button 
              link 
              type="danger" 
              size="small" 
              @click="handleTerminate(row)"
              :disabled="row.status !== 2"
            >
              终止
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 下发合同对话框（第一步：填写信息） -->
    <el-dialog
      v-model="showIssueDialog"
      title="下发合同 - 填写信息"
      width="600px"
      @close="handleIssueDialogClose"
    >
      <el-form
        ref="issueFormRef"
        :model="issueForm"
        :rules="issueRules"
        label-width="100px"
      >
        <el-form-item label="选择员工" prop="employeeId">
          <el-select 
            v-model="issueForm.employeeId" 
            placeholder="请选择员工" 
            filterable
            clearable
            style="width: 100%"
            @focus="handleEmployeeSelectFocus"
          >
            <el-option
              v-for="emp in employees"
              :key="emp.id"
              :label="`${emp.realName} (${emp.phone})`"
              :value="emp.id"
            >
              <span>{{ emp.realName }}</span>
              <span style="color: #8492a6; font-size: 13px; margin-left: 8px">{{ emp.phone }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="合同模板" prop="templateId">
          <el-select v-model="issueForm.templateId" placeholder="请选择合同模板" style="width: 100%">
            <el-option
              v-for="template in templates"
              :key="template.id"
              :label="template.templateName"
              :value="template.id"
            >
              <span>{{ template.templateName }}</span>
              <span v-if="template.remark" style="color: #8492a6; font-size: 12px; margin-left: 8px">
                ({{ template.remark }})
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="岗位" prop="position">
          <el-input v-model="issueForm.position" placeholder="请输入岗位" />
        </el-form-item>
        <el-form-item label="基本工资">
          <el-input-number
            v-model="issueForm.baseSalary"
            :min="0"
            :precision="2"
            placeholder="请输入基本工资"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="issueForm.startDate"
            type="date"
            placeholder="选择开始日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker
            v-model="issueForm.endDate"
            type="date"
            placeholder="选择结束日期（可选）"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handleCancelIssue">取消</el-button>
        <el-button type="primary" @click="goToSignature">
          下一步：签名
        </el-button>
      </template>
    </el-dialog>

    <!-- 签名对话框（第二步：经理签名） -->
    <el-dialog
      v-model="showSignDialog"
      title="下发合同 - 经理签名"
      width="600px"
      @close="handleSignDialogClose"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-alert
        title="请在下方签名板上签名，签名后将立即下发合同给员工"
        type="info"
        :closable="false"
        style="margin-bottom: 20px"
      />
      
      <SignaturePad 
        ref="signaturePadRef" 
        :show-buttons="false"
        @confirm="handleSignatureConfirm" 
      />
      
      <template #footer>
        <el-button @click="handleBackToForm">返回修改</el-button>
        <el-button @click="handleClearSignature">清除签名</el-button>
        <el-button type="primary" @click="handleManualConfirm" :loading="issueLoading">
          确认并下发
        </el-button>
      </template>
    </el-dialog>

    <!-- 合同详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="合同详情"
      width="700px"
    >
      <el-descriptions :column="2" border v-if="currentContract">
        <el-descriptions-item label="合同编号" :span="2">{{ currentContract.contractNo }}</el-descriptions-item>
        <el-descriptions-item label="合同类型">{{ currentContract.contractTypeDesc || currentContract.contractType }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentContract.status === 1" type="warning">待员工签字</el-tag>
          <el-tag v-else-if="currentContract.status === 2" type="success">已生效</el-tag>
          <el-tag v-else-if="currentContract.status === 3" type="info">已终止</el-tag>
          <el-tag v-else type="info">{{ currentContract.statusDesc || '未知' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="员工姓名">{{ currentContract.employeeName }}</el-descriptions-item>
        <el-descriptions-item label="发起人">{{ currentContract.initiatorName }}</el-descriptions-item>
        <el-descriptions-item label="岗位">{{ currentContract.position || '-' }}</el-descriptions-item>
        <el-descriptions-item label="基本工资">
          {{ currentContract.baseSalary ? `¥${currentContract.baseSalary}` : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ currentContract.startDate }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ currentContract.endDate || '无固定期限' }}</el-descriptions-item>
        <el-descriptions-item label="经理签署时间" :span="2">
          {{ currentContract.initiatorSignTime || '未签署' }}
        </el-descriptions-item>
        <el-descriptions-item label="下发时间" :span="2">
          {{ currentContract.issueTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="员工签署时间" :span="2">
          {{ currentContract.employeeSignTime || '未签署' }}
        </el-descriptions-item>
        <el-descriptions-item label="合同生效时间" :span="2">
          {{ currentContract.effectiveTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">
          {{ currentContract.createdAt }}
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
        <el-button type="primary" @click="handlePreviewPdf(currentContract)" :icon="View">
          预览PDF
        </el-button>
        <el-button 
          type="success" 
          @click="handleDownloadPdf(currentContract)" 
          :icon="Download"
          :disabled="currentContract.status !== 2"
        >
          下载PDF
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { Plus, View, Download } from '@element-plus/icons-vue'
import {
  issueContract,
  getMyContracts,
  getContractDetail,
  getAvailableTemplates,
  getContractPdfUrl,
  terminateContract,
  ContractDetailVO,
  ContractTemplate
} from '@/api/contract'
import { getMyEmployees, Employee } from '@/api/employee'
import SignaturePad from '@/components/SignaturePad.vue'

const loading = ref(false)
const issueLoading = ref(false)
const showIssueDialog = ref(false)
const showSignDialog = ref(false)
const showDetailDialog = ref(false)
const isGoingToSign = ref(false) // 标记是否正在前往签名步骤

const contracts = ref<ContractDetailVO[]>([])
const currentContract = ref<ContractDetailVO | null>(null)
const statusFilter = ref<number | undefined>(undefined)
const employees = ref<Employee[]>([])
const templates = ref<ContractTemplate[]>([])
const signatureBase64 = ref('')

const issueFormRef = ref<FormInstance>()
const signaturePadRef = ref<InstanceType<typeof SignaturePad>>()
const issueForm = ref({
  employeeId: null as number | null,
  templateId: 1,
  startDate: '',
  endDate: '',
  position: '',
  baseSalary: null as number | null
})

const issueRules: FormRules = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  templateId: [{ required: true, message: '请输入模板ID', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  position: [{ required: true, message: '请输入岗位', trigger: 'blur' }]
}

// 加载合同列表
const fetchContracts = async () => {
  loading.value = true
  try {
    const res: any = await getMyContracts(statusFilter.value)
    contracts.value = res.data || []
  } catch (error) {
    console.error('加载合同列表失败:', error)
    ElMessage.error('加载合同列表失败')
  } finally {
    loading.value = false
  }
}

// 加载员工列表
const fetchEmployees = async () => {
  try {
    const res: any = await getMyEmployees(1, 1000)
    console.log('员工列表响应:', res)
    
    // 处理不同的响应格式
    if (res.data) {
      if (Array.isArray(res.data)) {
        employees.value = res.data
      } else if (res.data.records && Array.isArray(res.data.records)) {
        employees.value = res.data.records
      } else if (res.data.list && Array.isArray(res.data.list)) {
        employees.value = res.data.list
      } else {
        employees.value = []
      }
    } else if (Array.isArray(res)) {
      employees.value = res
    } else {
      employees.value = []
    }
    
    console.log('加载到的员工数量:', employees.value.length)
    
    if (employees.value.length === 0) {
      ElMessage.warning('当前没有可选择的员工，请先注册员工')
    }
  } catch (error: any) {
    console.error('加载员工列表失败:', error)
    ElMessage.error(error.message || '加载员工列表失败')
    employees.value = []
  }
}

// 加载合同模板列表
const fetchTemplates = async () => {
  try {
    const res: any = await getAvailableTemplates()
    console.log('模板列表响应:', res)
    
    if (res.data && Array.isArray(res.data)) {
      templates.value = res.data
    } else {
      templates.value = []
    }
    
    console.log('加载到的模板数量:', templates.value.length)
    
    if (templates.value.length === 0) {
      ElMessage.warning('当前没有可用模板，请联系管理员添加')
    }
  } catch (error: any) {
    console.error('加载模板列表失败:', error)
    ElMessage.error(error.message || '加载模板列表失败')
    templates.value = []
  }
}

// 处理员工选择框聚焦事件
const handleEmployeeSelectFocus = () => {
  if (employees.value.length === 0) {
    console.log('员工列表为空，重新加载...')
    fetchEmployees()
  }
}

// 取消下发合同
const handleCancelIssue = () => {
  isGoingToSign.value = false
  showIssueDialog.value = false
  resetIssueForm()
}

// 处理表单对话框关闭
const handleIssueDialogClose = () => {
  console.log('表单对话框关闭')
  console.log('  - isGoingToSign:', isGoingToSign.value)
  console.log('  - 当前表单数据:', JSON.stringify(issueForm.value))
  
  // 如果是要进入签名步骤，不重置表单
  if (isGoingToSign.value) {
    console.log('保留表单数据，准备进入签名步骤')
    return
  }
  
  // 只有在真正取消时才重置
  console.log('重置表单')
  resetIssueForm()
}

// 处理签名对话框关闭
const handleSignDialogClose = () => {
  resetSignature()
}

// 返回表单修改
const handleBackToForm = () => {
  console.log('返回表单修改，当前表单数据:', issueForm.value)
  isGoingToSign.value = true // 设置标志，表示要重新打开表单对话框
  showSignDialog.value = false
  resetSignature()
  setTimeout(() => {
    showIssueDialog.value = true
    // 打开后重置标志
    setTimeout(() => {
      isGoingToSign.value = false
    }, 200)
  }, 100)
}

// 清除签名
const handleClearSignature = () => {
  if (signaturePadRef.value) {
    signaturePadRef.value.clear()
  }
}

// 手动确认签名（点击确认按钮）
const handleManualConfirm = () => {
  if (!signaturePadRef.value) {
    ElMessage.warning('签名组件未就绪')
    return
  }
  
  // 检查是否有签名
  if (!signaturePadRef.value.checkHasSignature()) {
    ElMessage.warning('请先在签名板上签名')
    return
  }
  
  // 获取签名并触发提交
  const signature = signaturePadRef.value.getSignature()
  if (signature) {
    handleSignatureConfirm(signature)
  } else {
    ElMessage.error('获取签名失败，请重新签名')
  }
}

// 进入签名步骤
const goToSignature = async () => {
  if (!issueFormRef.value) return

  // 先检查员工是否已选择
  if (!issueForm.value.employeeId) {
    ElMessage.warning('请先选择员工')
    return
  }

  await issueFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请完善表单信息')
      return
    }
    
    // 打印表单数据用于调试
    console.log('表单验证通过，当前数据:', {
      ...issueForm.value,
      employeeId: Number(issueForm.value.employeeId),
      employeeIdType: typeof issueForm.value.employeeId
    })
    
    // 设置标志，表示正在前往签名步骤
    isGoingToSign.value = true
    
    // 验证通过，进入签名步骤
    showIssueDialog.value = false
    
    // 延迟一下再打开签名对话框，避免对话框切换太快
    setTimeout(() => {
      showSignDialog.value = true
      // 打开签名对话框后重置标志
      setTimeout(() => {
        isGoingToSign.value = false
      }, 200)
    }, 100)
  })
}

// 确认签名并下发合同
const handleSignatureConfirm = async (signature: string) => {
  console.log('开始处理签名确认，当前表单数据:', issueForm.value)
  
  // 再次验证表单数据（这不应该失败，因为已经在第一步验证过了）
  if (!issueForm.value.employeeId) {
    ElMessage.error('数据异常：员工ID丢失，请返回重新填写')
    showSignDialog.value = false
    setTimeout(() => {
      showIssueDialog.value = true
    }, 100)
    return
  }
  
  if (!issueForm.value.position || issueForm.value.position.trim() === '') {
    ElMessage.error('数据异常：岗位信息丢失，请返回重新填写')
    showSignDialog.value = false
    setTimeout(() => {
      showIssueDialog.value = true
    }, 100)
    return
  }
  
  if (!issueForm.value.startDate) {
    ElMessage.error('数据异常：开始日期丢失，请返回重新填写')
    showSignDialog.value = false
    setTimeout(() => {
      showIssueDialog.value = true
    }, 100)
    return
  }
  
  signatureBase64.value = signature
    issueLoading.value = true
  
    try {
    // 确保 employeeId 是数字类型
    const employeeId = Number(issueForm.value.employeeId)
    if (isNaN(employeeId) || employeeId <= 0) {
      throw new Error('员工ID无效')
    }
    
    const requestData = {
      employeeId: employeeId,
        templateId: issueForm.value.templateId,
      position: issueForm.value.position.trim(),
      baseSalary: issueForm.value.baseSalary || undefined,
        startDate: issueForm.value.startDate,
        endDate: issueForm.value.endDate || undefined,
      signatureBase64: signature,
      ipAddress: undefined,
      deviceInfo: navigator.userAgent,
      sendNotification: true
    }
    
    console.log('下发合同请求数据:', requestData)
    
    await issueContract(requestData)
    
    ElMessage.success('合同签署并下发成功')
    isGoingToSign.value = false
    showSignDialog.value = false
    resetIssueForm()
      await fetchContracts()
  } catch (error: any) {
      console.error('下发合同失败:', error)
    ElMessage.error(error.message || '下发合同失败')
    // 保持在签名对话框，让用户可以重试或返回
    } finally {
      issueLoading.value = false
    }
}

// 查看详情
const handleDetail = async (row: ContractDetailVO) => {
  try {
    const res: any = await getContractDetail(row.id)
    currentContract.value = res.data
    showDetailDialog.value = true
  } catch (error) {
    console.error('获取合同详情失败:', error)
    ElMessage.error('获取合同详情失败')
  }
}

// 签署合同（废弃，新流程不使用）
const handleSign = (row: ContractDetailVO) => {
  ElMessage.info('新流程中，下发合同时已包含签名')
}

// 预览PDF
const handlePreviewPdf = async (contract: ContractDetailVO) => {
  if (!contract.contractPdfUrl) {
    ElMessage.warning('合同PDF不存在')
    return
  }

  try {
    const res: any = await getContractPdfUrl(contract.id)
    if (res.code === 200 && res.data) {
      // 在新窗口打开PDF
      window.open(res.data, '_blank')
    } else {
      ElMessage.error(res.message || '获取PDF失败')
    }
  } catch (error: any) {
    console.error('预览PDF失败:', error)
    ElMessage.error(error.message || '预览PDF失败')
  }
}

// 下载PDF
const handleDownloadPdf = async (contract: ContractDetailVO) => {
  if (!contract.contractPdfUrl) {
    ElMessage.warning('合同PDF不存在')
    return
  }

  if (contract.status !== 2) {
    ElMessage.warning('只能下载已生效的合同')
    return
  }

  try {
    const res: any = await getContractPdfUrl(contract.id)
    if (res.code === 200 && res.data) {
      // 创建下载链接
      const link = document.createElement('a')
      link.href = res.data
      link.download = `${contract.employeeName}_${contract.contractNo}.pdf`
      link.target = '_blank'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      ElMessage.success('开始下载合同')
    } else {
      ElMessage.error(res.message || '获取PDF失败')
    }
  } catch (error: any) {
    console.error('下载PDF失败:', error)
    ElMessage.error(error.message || '下载PDF失败')
  }
}

// 终止合同
const handleTerminate = async (row: ContractDetailVO) => {
  // 验证合同状态
  if (row.status !== 2) {
    ElMessage.warning('只能终止已生效的合同')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要终止员工 ${row.employeeName} 的合同吗？合同编号：${row.contractNo}`,
      '终止合同确认',
      {
        confirmButtonText: '确定终止',
        cancelButtonText: '取消',
        type: 'warning',
        center: true
      }
    )

    // 调用终止合同API
    await terminateContract(row.id)
    ElMessage.success('合同已终止')
    
    // 刷新列表
    await fetchContracts()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('终止合同失败:', error)
      ElMessage.error(error.message || '终止合同失败')
    }
  }
}

// 重置表单
const resetIssueForm = () => {
  console.log('执行 resetIssueForm')
  // 先重置表单数据
  issueForm.value = {
    employeeId: null,
    templateId: 1,
    startDate: '',
    endDate: '',
    position: '',
    baseSalary: null
  }
  signatureBase64.value = ''
  // 延迟重置表单验证状态，避免影响数据
  setTimeout(() => {
    issueFormRef.value?.clearValidate()
  }, 100)
}

// 重置签名
const resetSignature = () => {
  signatureBase64.value = ''
  if (signaturePadRef.value) {
    signaturePadRef.value.clear()
  }
}

onMounted(() => {
  fetchContracts()
  fetchEmployees()
  fetchTemplates()
})
</script>

<style scoped lang="scss">
.contract-container {
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

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  // No specific styles needed
}
</style>

