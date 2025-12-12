<template>
  <div class="contract-container">
    <el-card class="header-card">
      <div class="header-content">
        <div>
          <h2>合同管理</h2>
          <p class="subtitle">查看各经理下员工的合同信息</p>
        </div>
      </div>
    </el-card>

    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="选择经理">
          <el-select
            v-model="filterForm.managerId"
            placeholder="请选择经理"
            clearable
            style="width: 200px"
            @change="handleManagerChange"
          >
            <el-option
              v-for="manager in managerList"
              :key="manager.id"
              :label="manager.realName"
              :value="manager.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="合同状态">
          <el-select
            v-model="filterForm.status"
            placeholder="全部状态"
            clearable
            style="width: 150px"
            @change="loadContracts"
          >
            <el-option label="草稿" :value="0" />
            <el-option label="待签署" :value="1" />
            <el-option label="已签署" :value="2" />
            <el-option label="已拒绝" :value="3" />
            <el-option label="已过期" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadContracts" :icon="Search">
            查询
          </el-button>
          <el-button @click="handleReset" :icon="Refresh">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table 
        :data="contractList" 
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa' }"
      >
        <el-table-column prop="contractNo" label="合同编号" width="180" />
        <el-table-column prop="employeeName" label="员工姓名" width="120" />
        <el-table-column prop="position" label="岗位" width="150" />
        <el-table-column prop="baseSalary" label="基本工资" width="120">
          <template #default="{ row }">
            ¥{{ row.baseSalary?.toLocaleString() || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="合同开始日期" width="120" />
        <el-table-column prop="endDate" label="合同结束日期" width="120" />
        <el-table-column prop="statusDesc" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button 
              link 
              type="primary" 
              size="small" 
              @click="handleViewDetail(row)"
            >
              详情
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
          @size-change="loadContracts"
          @current-change="loadContracts"
        />
      </div>
    </el-card>

    <!-- 合同详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="合同详情"
      width="700px"
    >
      <el-descriptions :column="2" border v-if="currentContract">
        <el-descriptions-item label="合同编号">
          {{ currentContract.contractNo }}
        </el-descriptions-item>
        <el-descriptions-item label="合同类型">
          {{ currentContract.contractTypeDesc }}
        </el-descriptions-item>
        <el-descriptions-item label="员工姓名">
          {{ currentContract.employeeName }}
        </el-descriptions-item>
        <el-descriptions-item label="发起人">
          {{ currentContract.initiatorName }}
        </el-descriptions-item>
        <el-descriptions-item label="岗位">
          {{ currentContract.position }}
        </el-descriptions-item>
        <el-descriptions-item label="基本工资">
          ¥{{ currentContract.baseSalary?.toLocaleString() }}
        </el-descriptions-item>
        <el-descriptions-item label="合同开始日期">
          {{ currentContract.startDate }}
        </el-descriptions-item>
        <el-descriptions-item label="合同结束日期">
          {{ currentContract.endDate }}
        </el-descriptions-item>
        <el-descriptions-item label="合同状态">
          <el-tag :type="getStatusType(currentContract.status)">
            {{ currentContract.statusDesc }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(currentContract.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="发起人签署时间" v-if="currentContract.initiatorSignTime">
          {{ formatDateTime(currentContract.initiatorSignTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="下发时间" v-if="currentContract.issueTime">
          {{ formatDateTime(currentContract.issueTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="员工签署时间" v-if="currentContract.employeeSignTime">
          {{ formatDateTime(currentContract.employeeSignTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="合同生效时间" v-if="currentContract.effectiveTime">
          {{ formatDateTime(currentContract.effectiveTime) }}
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
        <el-button 
          type="primary" 
          @click="handleViewContract(currentContract!)"
          v-if="currentContract?.contractPdfUrl"
          :icon="View"
        >
          预览PDF
        </el-button>
        <el-button 
          type="success" 
          @click="handleDownloadPdf(currentContract!)"
          v-if="currentContract?.contractPdfUrl && currentContract?.status === 2"
          :icon="Download"
        >
          下载PDF
        </el-button>
      </template>
    </el-dialog>

    <!-- PDF预览对话框 (使用PDF.js) -->
    <el-dialog
      v-model="showPdfDialog"
      title="合同预览"
      width="1200px"
      top="3vh"
      @close="handlePreviewClose"
    >
      <div class="preview-container" v-if="currentContract">
        <!-- 文件预览区域 -->
        <div class="file-preview-area" v-loading="previewLoading">
          <div v-if="!currentContract.contractPdfUrl" class="no-file">
            <el-empty description="该合同暂无PDF文件" />
          </div>
          
          <div v-else-if="previewUrl" class="preview-content">
            <!-- PDF 预览容器 -->
            <div class="pdf-viewer-container">
              <!-- 工具栏 -->
              <div class="pdf-toolbar">
                <div class="toolbar-left">
                  <el-button-group>
                    <el-button size="small" @click="previousPage" :disabled="currentPage <= 1">
                      <el-icon><ArrowLeft /></el-icon>
                      上一页
                    </el-button>
                    <el-button size="small" @click="nextPage" :disabled="currentPage >= totalPages">
                      下一页
                      <el-icon><ArrowRight /></el-icon>
                    </el-button>
                  </el-button-group>
                  <span class="page-info">
                    第 {{ currentPage }} / {{ totalPages }} 页
                  </span>
                </div>
                <div class="toolbar-right">
                  <el-button-group>
                    <el-button size="small" @click="zoomOut" :disabled="scale <= 0.5">
                      <el-icon><ZoomOut /></el-icon>
                    </el-button>
                    <el-button size="small" disabled>{{ Math.round(scale * 100) }}%</el-button>
                    <el-button size="small" @click="zoomIn" :disabled="scale >= 3">
                      <el-icon><ZoomIn /></el-icon>
                    </el-button>
                  </el-button-group>
                </div>
              </div>
              
              <!-- PDF渲染区域 -->
              <div class="pdf-canvas-wrapper" ref="canvasWrapper">
                <canvas ref="pdfCanvas"></canvas>
              </div>
            </div>
          </div>
          
          <div v-else class="loading-preview">
            <el-icon class="is-loading"><Loading /></el-icon>
            <p>正在加载预览...</p>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, toRaw, markRaw } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, ArrowLeft, ArrowRight, ZoomIn, ZoomOut, Loading, View, Download } from '@element-plus/icons-vue'
import { getContractsByManager, getContractPdfUrl, ContractDetailVO } from '@/api/contract'
import { getManagerList, Manager } from '@/api/manager'
import * as pdfjsLib from 'pdfjs-dist'
import { useUserStore } from '@/stores/user'

// 配置PDF.js worker
pdfjsLib.GlobalWorkerOptions.workerSrc = `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjsLib.version}/pdf.worker.min.js`

const userStore = useUserStore()

const loading = ref(false)
const showDetailDialog = ref(false)
const showPdfDialog = ref(false)
const previewLoading = ref(false)

const contractList = ref<ContractDetailVO[]>([])
const managerList = ref<Manager[]>([])
const currentContract = ref<ContractDetailVO | null>(null)
const currentPdfUrl = ref('')
const previewUrl = ref('')

// PDF.js相关
const pdfCanvas = ref<HTMLCanvasElement | null>(null)
const canvasWrapper = ref<HTMLDivElement | null>(null)
const pdfDoc = ref<any>(null)
const currentPage = ref(1)
const totalPages = ref(0)
const scale = ref(1.5)

const filterForm = reactive({
  managerId: undefined as number | undefined,
  status: undefined as number | undefined
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 获取状态标签类型
const getStatusType = (status: number) => {
  const typeMap: Record<number, any> = {
    0: 'info',      // 草稿
    1: 'warning',   // 待签署
    2: 'success',   // 已签署
    3: 'danger',    // 已拒绝
    4: 'info'       // 已过期
  }
  return typeMap[status] || 'info'
}

// 格式化日期时间
const formatDateTime = (dateTime: string | undefined) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 加载经理列表
const loadManagers = async () => {
  try {
    const res: any = await getManagerList(1, 1000)
    managerList.value = res.data.records.filter((m: Manager) => m.status === 1)
  } catch (error) {
    console.error('加载经理列表失败:', error)
    ElMessage.error('加载经理列表失败')
  }
}

// 加载合同列表
const loadContracts = async () => {
  if (!filterForm.managerId) {
    ElMessage.warning('请先选择经理')
    return
  }

  loading.value = true
  try {
    const res: any = await getContractsByManager(
      filterForm.managerId,
      pagination.pageNum,
      pagination.pageSize,
      filterForm.status
    )
    contractList.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('加载合同列表失败:', error)
    ElMessage.error('加载合同列表失败')
  } finally {
    loading.value = false
  }
}

// 经理选择变化
const handleManagerChange = () => {
  pagination.pageNum = 1
  if (filterForm.managerId) {
    loadContracts()
  } else {
    contractList.value = []
    pagination.total = 0
  }
}

// 重置筛选
const handleReset = () => {
  filterForm.managerId = undefined
  filterForm.status = undefined
  contractList.value = []
  pagination.pageNum = 1
  pagination.total = 0
}

// 查看合同详情
const handleViewDetail = (row: ContractDetailVO) => {
  currentContract.value = row
  showDetailDialog.value = true
}

// 查看合同PDF
const handleViewContract = async (row: ContractDetailVO) => {
  if (!row.contractPdfUrl) {
    ElMessage.warning('合同PDF文件不存在')
    return
  }
  
  currentContract.value = row
  showPdfDialog.value = true
  
  // 生成预览URL并加载PDF
  await generatePreviewUrl(row.contractPdfUrl)
}

// 生成预览URL并加载PDF
const generatePreviewUrl = async (fileUrl: string) => {
  previewLoading.value = true
  previewUrl.value = ''
  
  try {
    // 从完整URL中提取OSS路径
    const ossPath = extractOssPath(fileUrl)
    
    // 使用后端代理URL，解决CORS问题
    const proxyUrl = `/api/hr/contract/proxy-file?ossPath=${encodeURIComponent(ossPath)}`
    previewUrl.value = proxyUrl
    
    console.log('使用代理URL:', proxyUrl)
    
    // 等待DOM更新后加载PDF
    await nextTick()
    await loadPdf(proxyUrl)
  } catch (error) {
    console.error('生成预览URL失败:', error)
    ElMessage.error('生成预览URL失败')
  } finally {
    previewLoading.value = false
  }
}

// 从URL中提取OSS路径
const extractOssPath = (fileUrl: string): string => {
  if (!fileUrl) {
    throw new Error('文件URL为空')
  }
  
  // 如果已经是纯路径，直接返回
  if (!fileUrl.startsWith('http')) {
    return fileUrl
  }
  
  try {
    // 从完整URL中提取路径部分（去除域名）
    const url = new URL(fileUrl)
    // 返回路径，去除开头的斜杠
    return url.pathname.startsWith('/') ? url.pathname.substring(1) : url.pathname
  } catch (error) {
    console.error('解析URL失败:', error)
    return fileUrl
  }
}

// 加载PDF文件
const loadPdf = async (url: string) => {
  try {
    console.log('开始加载PDF:', url)
    
    // 构建完整URL（如果是相对路径）
    const fullUrl = url.startsWith('http') ? url : `http://localhost:8085${url}`
    
    // 通过fetch获取PDF二进制数据（会带上Authorization头）
    const response = await fetch(fullUrl, {
      method: 'GET',
      headers: {
        'Authorization': userStore.token ? `Bearer ${userStore.token}` : ''
      }
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const arrayBuffer = await response.arrayBuffer()
    console.log('PDF数据加载成功，大小:', arrayBuffer.byteLength, 'bytes')
    
    // 使用二进制数据加载PDF
    const loadingTask = pdfjsLib.getDocument({
      data: new Uint8Array(arrayBuffer),
      cMapUrl: `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjsLib.version}/cmaps/`,
      cMapPacked: true
    })
    
    const pdf = await loadingTask.promise
    // 使用markRaw防止PDF对象被Vue的响应式系统包装
    pdfDoc.value = markRaw(pdf)
    totalPages.value = pdf.numPages
    currentPage.value = 1
    
    console.log('PDF加载成功，总页数:', totalPages.value)
    
    // 渲染第一页
    await renderPage(1)
  } catch (error) {
    console.error('PDF加载失败:', error)
    ElMessage.error('PDF加载失败: ' + (error as Error).message)
  }
}

// 渲染指定页
const renderPage = async (pageNum: number) => {
  const pdf = pdfDoc.value
  const canvas = pdfCanvas.value
  
  if (!pdf || !canvas) {
    console.warn('PDF文档或Canvas未准备好')
    return
  }
  
  try {
    // 使用toRaw获取原始非响应式对象
    const rawPdf = toRaw(pdf)
    
    // 获取页面
    const page = await rawPdf.getPage(pageNum)
    
    // 获取设备像素比，用于高清渲染
    const dpr = window.devicePixelRatio || 1
    
    // 获取基础视口（CSS尺寸）
    const viewport = page.getViewport({ scale: scale.value })
    
    // 准备Canvas
    const context = canvas.getContext('2d')
    
    if (!context) {
      throw new Error('无法获取Canvas上下文')
    }
    
    // 设置Canvas显示尺寸（CSS尺寸，用户看到的大小）
    canvas.style.width = `${viewport.width}px`
    canvas.style.height = `${viewport.height}px`
    
    // 设置Canvas实际像素尺寸（物理像素，考虑DPR以实现高清）
    canvas.width = viewport.width * dpr
    canvas.height = viewport.height * dpr
    
    // 缩放Canvas上下文以匹配物理像素
    context.scale(dpr, dpr)
    
    // 渲染PDF页面
    const renderContext = {
      canvasContext: context,
      viewport: viewport
    }
    
    await page.render(renderContext).promise
    console.log('页面渲染成功:', pageNum)
  } catch (error) {
    console.error('页面渲染失败:', error)
    ElMessage.error('页面渲染失败')
  }
}

// 上一页
const previousPage = async () => {
  if (currentPage.value > 1) {
    currentPage.value--
    await renderPage(currentPage.value)
  }
}

// 下一页
const nextPage = async () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    await renderPage(currentPage.value)
  }
}

// 放大
const zoomIn = async () => {
  if (scale.value < 3) {
    scale.value += 0.25
    await renderPage(currentPage.value)
  }
}

// 缩小
const zoomOut = async () => {
  if (scale.value > 0.5) {
    scale.value -= 0.25
    await renderPage(currentPage.value)
  }
}

// 关闭预览对话框
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

const handlePreviewClose = () => {
  previewUrl.value = ''
  currentPdfUrl.value = ''
  pdfDoc.value = null
  currentPage.value = 1
  totalPages.value = 0
  scale.value = 1.5
}

onMounted(() => {
  loadManagers()
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
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}

// PDF预览样式
.preview-container {
  height: 80vh;
  display: flex;
  flex-direction: column;
}

.file-preview-area {
  flex: 1;
  overflow: auto;
  background: #f5f7fa;
  border-radius: 4px;
}

.no-file {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.preview-content {
  height: 100%;
}

.pdf-viewer-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #525659;
}

.pdf-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: #323639;
  color: #fff;

  .toolbar-left,
  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .page-info {
    margin-left: 10px;
    font-size: 14px;
  }
}

.pdf-canvas-wrapper {
  flex: 1;
  overflow: auto;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 20px;

  canvas {
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);
    background: white;
  }
}

.loading-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;

  .el-icon {
    font-size: 48px;
    margin-bottom: 10px;
  }

  p {
    font-size: 14px;
  }
}
</style>
