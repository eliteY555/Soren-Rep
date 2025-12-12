<template>
  <div class="template-container">
    <el-card class="header-card">
      <div class="header-content">
        <div>
          <h2>模板管理</h2>
          <p class="subtitle">管理合同模板文件</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" @click="showEditDialog(null)" :icon="Plus">
            创建/更新合同模板
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table 
        :data="templateList" 
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa' }"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="templateName" label="模板名称" min-width="200" />
        <el-table-column prop="remark" label="描述" min-width="250" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small">启用</el-tag>
            <el-tag v-else type="info" size="small">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="230">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handlePreview(row)">
              查看模板
            </el-button>
            <el-button link type="success" size="small" @click="handleDownload(row)" :icon="Download">
              下载
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              删除
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
          @size-change="loadTemplates"
          @current-change="loadTemplates"
        />
      </div>
    </el-card>

    <!-- 创建/更新模板对话框 -->
    <el-dialog
      v-model="showFormDialog"
      title="创建合同模板"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="上传文件" prop="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
            :before-upload="beforeUpload"
            :file-list="fileList"
            accept=".pdf,application/pdf"
          >
            <el-button type="primary" :icon="Upload">选择三方合同模板</el-button>
            <template #tip>
              <div class="el-upload__tip">
                仅支持PDF格式，文件大小不超过20MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showFormDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 预览模板对话框 -->
    <el-dialog
      v-model="showPreviewDialog"
      title="查看模板"
      width="1200px"
      top="3vh"
      @close="handlePreviewClose"
    >
      <div class="preview-container" v-if="currentTemplate">
        <!-- 文件预览区域 -->
        <div class="file-preview-area" v-loading="previewLoading">
          <div v-if="!currentTemplate.ossFilePath" class="no-file">
            <el-empty description="该模板暂无文件" />
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
import { ElMessage, ElMessageBox, FormInstance, FormRules, UploadFile } from 'element-plus'
import { Plus, Upload, Download, View, Loading, ArrowLeft, ArrowRight, ZoomIn, ZoomOut } from '@element-plus/icons-vue'
import * as pdfjsLib from 'pdfjs-dist'
import { useUserStore } from '@/stores/user'
import {
  uploadTemplate,
  getTemplateList,
  getTemplateDetail,
  deleteTemplate,
  getTemplatePreviewUrl,
  ContractTemplate
} from '@/api/template'

// 配置PDF.js worker
pdfjsLib.GlobalWorkerOptions.workerSrc = `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjsLib.version}/pdf.worker.min.js`

const userStore = useUserStore()

const loading = ref(false)
const submitLoading = ref(false)
const previewLoading = ref(false)
const showFormDialog = ref(false)
const showPreviewDialog = ref(false)
const isEdit = ref(false)

const templateList = ref<ContractTemplate[]>([])
const currentTemplate = ref<ContractTemplate | null>(null)
const fileList = ref<UploadFile[]>([])
const selectedFile = ref<File | null>(null)
const previewUrl = ref('')

// PDF.js相关
const pdfCanvas = ref<HTMLCanvasElement | null>(null)
const canvasWrapper = ref<HTMLDivElement | null>(null)
const pdfDoc = ref<any>(null)
const currentPage = ref(1)
const totalPages = ref(0)
const scale = ref(1.5)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const formRef = ref<FormInstance>()
const formData = reactive({
  id: undefined as number | undefined,
  templateName: '',
  remark: '',
  status: 1
})

const formRules: FormRules = {
  file: [{ required: true, message: '请选择PDF文件', trigger: 'change' }]
}

// 加载模板列表
const loadTemplates = async () => {
  loading.value = true
  try {
    const res: any = await getTemplateList(pagination.pageNum, pagination.pageSize)
    templateList.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('加载模板列表失败:', error)
    ElMessage.error('加载模板列表失败')
  } finally {
    loading.value = false
  }
}

// 文件选择前验证
const beforeUpload = (file: File) => {
  // 验证文件类型
  const isPDF = file.type === 'application/pdf'
  const isValidExtension = file.name.toLowerCase().endsWith('.pdf')
  
  if (!isPDF || !isValidExtension) {
    ElMessage.error('只能上传PDF格式的文件！')
    return false
  }
  
  // 验证文件大小（20MB）
  const isLt20M = file.size / 1024 / 1024 < 20
  if (!isLt20M) {
    ElMessage.error('文件大小不能超过20MB！')
    return false
  }
  
  return true
}

// 文件选择
const handleFileChange = (file: UploadFile) => {
  if (file.raw) {
    // 再次验证文件类型
    const isPDF = file.raw.type === 'application/pdf'
    const isValidExtension = file.raw.name.toLowerCase().endsWith('.pdf')
    
    if (!isPDF || !isValidExtension) {
      ElMessage.error('只能上传PDF格式的文件！')
      // 清除已选择的文件
      fileList.value = []
      selectedFile.value = null
      return
    }
    
    // 验证文件大小
    const isLt20M = file.raw.size / 1024 / 1024 < 20
    if (!isLt20M) {
      ElMessage.error('文件大小不能超过20MB！')
      fileList.value = []
      selectedFile.value = null
      return
    }
    
    selectedFile.value = file.raw
    console.log('文件选择成功:', file.name, '大小:', (file.size! / 1024 / 1024).toFixed(2) + 'MB')
  }
}

// 超出文件数量限制
const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先删除已选择的文件')
}

// 显示创建/编辑对话框
const showEditDialog = async (row: ContractTemplate | null) => {
  isEdit.value = !!row
  
  if (row) {
    // 编辑模式：加载详情
    try {
      const res: any = await getTemplateDetail(row.id)
      const template = res.data
      formData.id = template.id
      formData.templateName = template.templateName
      formData.remark = template.remark || ''
      formData.status = template.status
    } catch (error) {
      console.error('加载模板详情失败:', error)
      ElMessage.error('加载模板详情失败')
      return
    }
  } else {
    // 创建模式：重置表单
    resetForm()
  }
  
  showFormDialog.value = true
}

// 提交表单（创建）
const handleSubmit = async () => {
  // 验证是否选择了文件
  if (!selectedFile.value) {
    ElMessage.warning('请选择三方合同模板PDF文件')
    return
  }

  // 验证文件类型
  if (!selectedFile.value.name.toLowerCase().endsWith('.pdf')) {
    ElMessage.error('只能上传PDF格式的文件')
    return
  }

  submitLoading.value = true
  try {
    // 直接上传文件
    await uploadTemplate(selectedFile.value)
    ElMessage.success('模板上传成功')
    
    showFormDialog.value = false
    await loadTemplates()
  } catch (error: any) {
    console.error('上传失败:', error)
    // 显示后端返回的错误信息
    const errorMessage = error?.response?.data?.message || error?.message || '上传失败'
    ElMessage.error(errorMessage)
  } finally {
    submitLoading.value = false
  }
}

// 查看模板（预览）
const handlePreview = async (row: ContractTemplate) => {
  try {
    const res: any = await getTemplateDetail(row.id)
    currentTemplate.value = res.data
    showPreviewDialog.value = true
    
    // 如果有文件URL，生成预览URL
    if (currentTemplate.value.ossFilePath) {
      await generatePreviewUrl(currentTemplate.value.ossFilePath)
    }
  } catch (error) {
    console.error('获取详情失败:', error)
    ElMessage.error('获取详情失败')
  }
}

// 生成预览URL并加载PDF
const generatePreviewUrl = async (fileUrl: string) => {
  previewLoading.value = true
  previewUrl.value = ''
  
  try {
    // 从完整URL中提取OSS路径
    const ossPath = extractOssPath(fileUrl)
    
    // 使用后端代理URL，解决CORS问题
    const proxyUrl = `/api/hr/template/proxy-file?ossPath=${encodeURIComponent(ossPath)}`
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
    console.log('PDF对象类型:', typeof pdf, pdf)
    
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
    // 使用toRaw获取原始非响应式对象（防止Proxy包装导致的私有字段访问问题）
    const rawPdf = toRaw(pdf)
    console.log('开始渲染页面:', pageNum, 'PDF对象:', rawPdf)
    
    // 获取页面 - 使用原始对象的方法
    let page
    try {
      page = await rawPdf.getPage(pageNum)
      console.log('成功获取页面对象:', page)
    } catch (e) {
      console.error('getPage失败:', e)
      throw e
    }
    
    // 获取设备像素比，用于高清渲染
    const dpr = window.devicePixelRatio || 1
    console.log('设备像素比:', dpr)
    
    // 获取基础视口（CSS尺寸）
    const viewport = page.getViewport({ scale: scale.value })
    console.log('基础视口信息:', viewport.width, 'x', viewport.height)
    
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
    
    console.log('Canvas CSS尺寸:', viewport.width, 'x', viewport.height)
    console.log('Canvas实际像素:', canvas.width, 'x', canvas.height)
    
    // 渲染PDF页面到Canvas
    const renderContext = {
      canvasContext: context,
      viewport: viewport
    }
    
    // 取消之前的渲染任务（如果有）
    if ((window as any).currentRenderTask) {
      try {
        (window as any).currentRenderTask.cancel()
      } catch (e) {
        console.log('取消渲染任务失败:', e)
      }
    }
    
    const renderTask = page.render(renderContext)
    ;(window as any).currentRenderTask = renderTask
    
    await renderTask.promise
    ;(window as any).currentRenderTask = null
    
    console.log('页面渲染成功:', pageNum, '尺寸:', canvas.width, 'x', canvas.height)
  } catch (error: any) {
    // 忽略取消错误
    if (error?.name === 'RenderingCancelledException') {
      console.log('渲染被取消')
      return
    }
    console.error('页面渲染失败，详细信息:', error)
    console.error('错误堆栈:', error.stack)
    ElMessage.error('页面渲染失败: ' + error.message)
  }
}

// 上一页
const previousPage = () => {
  if (currentPage.value <= 1) return
  currentPage.value--
  renderPage(currentPage.value)
}

// 下一页
const nextPage = () => {
  if (currentPage.value >= totalPages.value) return
  currentPage.value++
  renderPage(currentPage.value)
}

// 放大
const zoomIn = () => {
  if (scale.value >= 3) return
  scale.value += 0.25
  renderPage(currentPage.value)
}

// 缩小
const zoomOut = () => {
  if (scale.value <= 0.5) return
  scale.value -= 0.25
  renderPage(currentPage.value)
}

// 从完整URL中提取OSS路径
const extractOssPath = (fileUrl: string): string => {
  try {
    // 如果是完整的URL，提取路径部分
    if (fileUrl.startsWith('http')) {
      const url = new URL(fileUrl)
      // 移除开头的斜杠
      return url.pathname.substring(1)
    }
    // 如果已经是路径，直接返回
    return fileUrl
  } catch (error) {
    console.error('提取OSS路径失败:', error)
    return fileUrl
  }
}

// 从表格操作栏直接下载文件
const handleDownload = async (row: ContractTemplate) => {
  try {
    if (!row.ossFilePath) {
      ElMessage.warning('该模板暂无文件')
      return
    }
    
    ElMessage.info('正在准备下载...')
    
    // 构建代理下载URL
    const proxyUrl = `/api/hr/template/proxy-file?ossPath=${encodeURIComponent(row.ossFilePath)}`
    
    // 使用fetch下载文件
    const response = await fetch(`http://localhost:8085${proxyUrl}`, {
      method: 'GET',
      headers: {
        'Authorization': userStore.token ? `Bearer ${userStore.token}` : ''
      }
    })
    
    if (!response.ok) {
      throw new Error('下载失败')
    }
    
    // 获取blob数据
    const blob = await response.blob()
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${row.templateName}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败: ' + (error as Error).message)
  }
}

// 下载文件（从预览窗口）
const downloadFile = (url: string) => {
  // 通过代理URL下载，添加download参数
  const downloadUrl = url + '&download=true'
  const link = document.createElement('a')
  link.href = downloadUrl
  link.download = currentTemplate.value?.templateName + '.pdf' || 'template.pdf'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 在新窗口打开文件
const openFileInNewTab = (url: string) => {
  window.open(url, '_blank')
}

// 关闭预览对话框
const handlePreviewClose = () => {
  previewUrl.value = ''
  currentTemplate.value = null
  pdfDoc.value = null
  currentPage.value = 1
  totalPages.value = 0
  scale.value = 1.5
}

// 删除
const handleDelete = async (row: ContractTemplate) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模板"${row.templateName}"吗？此操作不可恢复！`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }
    )

    await deleteTemplate(row.id)
    ElMessage.success('删除成功')
    await loadTemplates()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  fileList.value = []
  selectedFile.value = null
  Object.assign(formData, {
    id: undefined,
    templateName: '',
    remark: '',
    status: 1
  })
}

onMounted(() => {
  loadTemplates()
})
</script>

<style scoped lang="scss">
.template-container {
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

  .header-actions {
    display: flex;
    gap: 10px;
  }
}

.table-card {
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}

// 预览容器
.preview-container {
  .file-preview-area {
    background: #f5f7fa;
    border-radius: 4px;
    position: relative;

    .no-file {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 400px;
    }

    .preview-content {
      .pdf-viewer-container {
        display: flex;
        flex-direction: column;
        height: 85vh;
        max-height: 85vh;
        background: #525252;
        border-radius: 4px;
        overflow: hidden;

        .pdf-toolbar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 10px 20px;
          background: #333;
          border-bottom: 1px solid #444;
          flex-shrink: 0;

          .toolbar-left,
          .toolbar-right {
            display: flex;
            align-items: center;
            gap: 15px;
          }

          .page-info {
            color: #fff;
            font-size: 14px;
            padding: 0 10px;
          }
        }

        .pdf-canvas-wrapper {
          flex: 1;
          overflow: auto;
          display: flex;
          justify-content: center;
          align-items: flex-start;
          padding: 20px;
          background: #525252;

          canvas {
            display: block;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
            background: white;
          }
        }
      }
    }

    .loading-preview {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 400px;
      color: #999;

      .el-icon {
        font-size: 40px;
        margin-bottom: 15px;
      }

      p {
        font-size: 16px;
      }
    }
  }
}

.file-link {
  color: #409eff;
  text-decoration: none;
  word-break: break-all;
  
  &:hover {
    text-decoration: underline;
  }
}
</style>

