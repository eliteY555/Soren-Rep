# OSS PDF预览实现方案

## 一、方案概述

本文档详细说明了如何实现从阿里云OSS存储的PDF文件在前端页面中的预览功能。由于直接从OSS获取文件存在CORS（跨域资源共享）问题，我们采用**后端代理**的方式来解决。

## 二、技术架构

### 2.1 技术栈
- **后端**: Spring Boot + 阿里云OSS SDK
- **前端**: Vue 3 + PDF.js + TypeScript
- **文件存储**: 阿里云OSS

### 2.2 核心流程

```
前端请求预览
    ↓
后端代理接口接收请求
    ↓
后端从OSS获取文件URL
    ↓
后端下载OSS文件流
    ↓
后端设置CORS响应头
    ↓
后端将文件流返回给前端
    ↓
前端使用PDF.js渲染
```

## 三、后端实现

### 3.1 Controller层 - 代理接口

文件路径: `back/tailai-hr-web/src/main/java/com/tailai/hr/controller/TemplateController.java`

```java
@Operation(summary = "代理下载模板文件", description = "通过后端代理下载模板文件，解决CORS问题")
@GetMapping("/proxy-file")
public void proxyFile(
        @Parameter(description = "OSS文件路径", required = true) 
        @RequestParam String ossPath,
        javax.servlet.http.HttpServletResponse response,
        @RequestHeader(value = "Authorization", required = false) String token) {
    
    log.info("收到文件代理请求，ossPath: {}", ossPath);
    templateService.proxyFile(ossPath, response);
}
```

**关键点**：
- 使用 `@GetMapping` 暴露代理接口
- 参数 `ossPath` 接收OSS文件路径
- 直接操作 `HttpServletResponse` 对象来流式传输文件
- 接收 `Authorization` header用于日志记录（实际认证由拦截器处理）

### 3.2 Service层 - 文件代理逻辑

文件路径: `back/tailai-hr-web/src/main/java/com/tailai/hr/service/TemplateService.java`

```java
public void proxyFile(String ossPath, HttpServletResponse response) {
    log.info("代理下载文件，OSS路径：{}", ossPath);
    
    InputStream inputStream = null;
    OutputStream outputStream = null;
    HttpURLConnection connection = null;
    
    try {
        // 1. 参数验证
        if (ossPath == null || ossPath.trim().isEmpty()) {
            throw new RuntimeException("OSS文件路径不能为空");
        }
        
        // 2. 调用文件服务获取临时访问URL（60秒有效期）
        Result<String> urlResult = fileServiceClient.getDownloadUrl(ossPath, 60);
        if (!urlResult.isSuccess() || urlResult.getData() == null) {
            throw new RuntimeException("获取文件URL失败：" + urlResult.getMessage());
        }
        
        String fileUrl = urlResult.getData();
        log.info("OSS文件URL：{}", fileUrl);
        
        // 3. 建立HTTP连接到OSS
        connection = (HttpURLConnection) new URL(fileUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);  // 连接超时10秒
        connection.setReadTimeout(30000);     // 读取超时30秒
        connection.connect();
        
        // 4. 检查响应状态
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("OSS文件下载失败，状态码：" + responseCode);
        }
        
        // 5. 获取文件输入流
        inputStream = connection.getInputStream();
        
        // 6. 设置响应头（关键：解决CORS问题）
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline");  // inline表示预览，attachment表示下载
        response.setHeader("Access-Control-Allow-Origin", "*");  // 允许所有域访问
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "*");
        
        // 7. 流式传输文件到前端
        outputStream = response.getOutputStream();
        byte[] buffer = new byte[8192];  // 8KB缓冲区
        int bytesRead;
        
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        
        outputStream.flush();
        log.info("文件代理下载成功");
        
    } catch (Exception e) {
        log.error("代理下载文件失败", e);
        try {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("文件下载失败：" + e.getMessage());
        } catch (IOException ex) {
            log.error("写入错误响应失败", ex);
        }
    } finally {
        // 8. 关闭所有资源
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error("关闭输入流失败", e);
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                log.error("关闭输出流失败", e);
            }
        }
        if (connection != null) {
            connection.disconnect();
        }
    }
}
```

**关键点**：
1. **参数验证**: 确保OSS路径不为空
2. **获取临时URL**: 通过文件服务获取带签名的临时访问URL
3. **HTTP连接**: 使用 `HttpURLConnection` 连接到OSS
4. **响应头设置**: 
   - `Content-Type: application/pdf` - 指定文件类型
   - `Content-Disposition: inline` - 浏览器内预览而非下载
   - `Access-Control-Allow-Origin: *` - **核心：解决CORS跨域问题**
5. **流式传输**: 使用缓冲区逐块读取和写入，避免大文件占用过多内存
6. **资源管理**: finally块确保所有流和连接都被正确关闭

### 3.3 FileService层 - OSS URL生成

文件路径: `back/tailai-file-service/src/main/java/com/tailai/file/service/OssService.java`

```java
public String generateTemporaryUrl(String ossPath, int expirationMinutes) {
    try {
        log.info("生成临时访问URL - 路径：{}，有效期：{}分钟", ossPath, expirationMinutes);
        
        // 1. 计算过期时间
        Date expiration = new Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000);
        
        // 2. 创建URL请求（不设置额外的响应头，使用上传时设置的）
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, ossPath)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);
        
        // 注意：不在这里设置Content-Type和Content-Disposition
        // 因为OSS不允许在URL生成时覆盖这些响应头
        // 这些响应头应该在文件上传时通过ObjectMetadata设置
        
        // 3. 生成签名URL
        URL url = ossClient.generatePresignedUrl(request);
        String urlString = url.toString();
        
        log.info("临时URL生成成功：{}", urlString);
        return urlString;
        
    } catch (Exception e) {
        log.error("生成临时URL失败", e);
        throw new RuntimeException("生成临时访问URL失败: " + e.getMessage());
    }
}
```

**关键点**：
- 生成带签名的临时访问URL（60秒有效期）
- **不在URL生成时设置响应头**，避免 `InvalidRequest` 错误
- 响应头应在文件上传时通过 `ObjectMetadata` 设置

### 3.4 文件上传时的配置

```java
public String uploadFile(MultipartFile file, String ossPath) throws IOException {
    // ... 其他代码
    
    // 设置文件元数据
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(file.getSize());
    metadata.setContentType(contentType);  // 设置正确的Content-Type
    
    // 对于可预览的文件（PDF、图片等），设置为inline
    if (isPreviewableFile(ossPath)) {
        metadata.setContentDisposition("inline");
        log.info("文件设置为预览模式（inline）：{}", ossPath);
    }
    
    // 上传文件
    ossClient.putObject(bucketName, ossPath, file.getInputStream(), metadata);
    
    // ... 其他代码
}

private boolean isPreviewableFile(String filePath) {
    String lowerPath = filePath.toLowerCase();
    return lowerPath.endsWith(".pdf") 
        || lowerPath.endsWith(".png") 
        || lowerPath.endsWith(".jpg") 
        || lowerPath.endsWith(".jpeg") 
        || lowerPath.endsWith(".gif") 
        || lowerPath.endsWith(".bmp") 
        || lowerPath.endsWith(".webp");
}
```

**关键点**：
- 在**上传时**设置 `Content-Disposition: inline`
- 这样后续访问文件时，OSS会自动返回正确的响应头

## 四、前端实现

### 4.1 API层 - 定义接口

文件路径: `front-hr/src/api/template.ts`

```typescript
export interface ContractTemplate {
  id?: number
  templateName: string
  templateCode?: string
  ossFilePath?: string  // OSS文件路径
  remark?: string
  status?: number
  createdAt?: string
  updatedAt?: string
}

// 其他API函数...
```

### 4.2 组件层 - PDF预览实现

文件路径: `front-hr/src/views/template/TemplateList.vue`

#### 4.2.1 导入依赖

```vue
<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, toRaw, markRaw } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules, UploadFile } from 'element-plus'
import * as pdfjsLib from 'pdfjs-dist'
import { useUserStore } from '@/stores/user'

// 配置PDF.js worker
pdfjsLib.GlobalWorkerOptions.workerSrc = 
  `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjsLib.version}/pdf.worker.min.js`

const userStore = useUserStore()
</script>
```

**关键点**：
- 导入 `toRaw` 和 `markRaw` - **重要：防止Vue响应式包装导致PDF.js私有字段访问错误**
- 配置PDF.js的worker路径

#### 4.2.2 定义状态变量

```typescript
// PDF.js相关状态
const pdfCanvas = ref<HTMLCanvasElement | null>(null)
const canvasWrapper = ref<HTMLDivElement | null>(null)
const pdfDoc = ref<any>(null)          // PDF文档对象
const currentPage = ref(1)              // 当前页码
const totalPages = ref(0)               // 总页数
const scale = ref(1.5)                  // 缩放比例
const previewUrl = ref('')              // 预览URL
const previewLoading = ref(false)       // 加载状态
```

#### 4.2.3 生成预览URL

```typescript
// 生成预览URL（调用后端代理）
const generatePreviewUrl = async (ossPath: string) => {
  try {
    previewLoading.value = true
    
    // 构建代理URL - 通过后端代理访问OSS文件
    const proxyUrl = `/api/hr/template/proxy-file?ossPath=${encodeURIComponent(ossPath)}`
    
    console.log('使用代理URL:', proxyUrl)
    previewUrl.value = proxyUrl
    
    // 加载PDF
    await loadPdf(proxyUrl)
    
  } catch (error) {
    console.error('生成预览URL失败:', error)
    ElMessage.error('预览失败: ' + (error as Error).message)
  } finally {
    previewLoading.value = false
  }
}
```

**关键点**：
- 构建代理URL: `/api/hr/template/proxy-file?ossPath=...`
- 使用 `encodeURIComponent` 编码OSS路径
- 调用 `loadPdf` 加载PDF文档

#### 4.2.4 加载PDF文件

```typescript
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
    
    // ⚠️ 关键：使用markRaw防止PDF对象被Vue的响应式系统包装
    // 如果不使用markRaw，Vue会将PDF对象转为Proxy，导致无法访问私有字段
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
```

**关键点**：
1. **使用fetch**: 允许添加自定义header（Authorization）
2. **获取ArrayBuffer**: 将响应转为二进制数据
3. **转换为Uint8Array**: PDF.js需要的数据格式
4. **markRaw()**: **核心关键**，防止Vue响应式包装
5. **配置cMapUrl**: 支持中文字符显示

#### 4.2.5 渲染PDF页面

```typescript
// 渲染指定页
const renderPage = async (pageNum: number) => {
  const pdf = pdfDoc.value
  const canvas = pdfCanvas.value
  
  if (!pdf || !canvas) {
    console.warn('PDF文档或Canvas未准备好')
    return
  }
  
  try {
    // ⚠️ 关键：使用toRaw获取原始非响应式对象
    // 防止Proxy包装导致的私有字段访问问题
    const rawPdf = toRaw(pdf)
    
    console.log('开始渲染页面:', pageNum)
    
    // 获取页面对象
    const page = await rawPdf.getPage(pageNum)
    console.log('成功获取页面对象')
    
    // 获取视口（根据缩放比例）
    const viewport = page.getViewport({ scale: scale.value })
    console.log('视口信息:', viewport.width, 'x', viewport.height)
    
    // 准备Canvas
    const context = canvas.getContext('2d')
    if (!context) {
      throw new Error('无法获取Canvas上下文')
    }
    
    // 设置Canvas尺寸
    canvas.height = viewport.height
    canvas.width = viewport.width
    
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
    
    console.log('页面渲染成功')
    
  } catch (error: any) {
    // 忽略取消错误
    if (error?.name === 'RenderingCancelledException') {
      console.log('渲染被取消')
      return
    }
    console.error('页面渲染失败:', error)
    ElMessage.error('页面渲染失败: ' + error.message)
  }
}
```

**关键点**：
1. **toRaw()**: 从可能的Proxy中提取原始对象
2. **getPage()**: 获取指定页面
3. **getViewport()**: 根据缩放比例计算视口
4. **Canvas渲染**: 将PDF页面绘制到Canvas上
5. **任务取消**: 防止快速翻页时的渲染冲突

#### 4.2.6 翻页和缩放控制

```typescript
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

// 下载PDF
const downloadFile = (url: string) => {
  const link = document.createElement('a')
  const downloadUrl = url + '&download=true'
  link.href = downloadUrl
  link.download = currentTemplate.value?.templateName + '.pdf' || 'template.pdf'
  link.click()
}

// 在新窗口打开
const openFileInNewTab = (url: string) => {
  window.open(url, '_blank')
}
```

#### 4.2.7 模板HTML

```vue
<template>
  <el-dialog
    v-model="showPreviewDialog"
    title="查看模板"
    width="80%"
    @close="handlePreviewClose"
  >
    <div class="preview-container">
      <div class="file-preview-area" v-loading="previewLoading">
        <div v-if="!currentTemplate.ossFilePath" class="no-file">
          <el-empty description="该模板暂无文件" />
        </div>
        <div v-else-if="previewUrl" class="preview-content">
          <!-- PDF查看器 -->
          <div class="pdf-viewer-container">
            <!-- 工具栏 -->
            <div class="pdf-toolbar">
              <div class="toolbar-left">
                <el-button 
                  size="small" 
                  :icon="ArrowLeft" 
                  :disabled="currentPage <= 1"
                  @click="previousPage"
                >
                  上一页
                </el-button>
                <span class="page-info">
                  {{ currentPage }} / {{ totalPages }}
                </span>
                <el-button 
                  size="small" 
                  :icon="ArrowRight" 
                  :disabled="currentPage >= totalPages"
                  @click="nextPage"
                >
                  下一页
                </el-button>
              </div>
              
              <div class="toolbar-right">
                <el-button 
                  size="small" 
                  :icon="ZoomOut"
                  :disabled="scale <= 0.5"
                  @click="zoomOut"
                >
                  缩小
                </el-button>
                <span class="zoom-info">{{ Math.round(scale * 100) }}%</span>
                <el-button 
                  size="small" 
                  :icon="ZoomIn"
                  :disabled="scale >= 3"
                  @click="zoomIn"
                >
                  放大
                </el-button>
                <el-button 
                  size="small" 
                  type="primary"
                  @click="downloadFile(previewUrl)"
                >
                  下载PDF
                </el-button>
                <el-button 
                  size="small"
                  @click="openFileInNewTab(previewUrl)"
                >
                  在新窗口打开
                </el-button>
              </div>
            </div>
            
            <!-- Canvas画布 -->
            <div class="pdf-canvas-wrapper" ref="canvasWrapper">
              <canvas ref="pdfCanvas" class="pdf-canvas"></canvas>
            </div>
          </div>
        </div>
        <div v-else class="loading-preview">
          <el-icon :size="50" class="is-loading"><Loading /></el-icon>
          <p>正在加载预览...</p>
        </div>
      </div>
    </div>
  </el-dialog>
</template>
```

#### 4.2.8 样式定义

```vue
<style scoped lang="scss">
.pdf-viewer-container {
  display: flex;
  flex-direction: column;
  height: 70vh;
  min-height: 500px;
  background: #525252;

  .pdf-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 20px;
    background: #333;
    border-bottom: 1px solid #444;
    color: #fff;

    .toolbar-left,
    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .page-info,
    .zoom-info {
      padding: 0 10px;
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
    background: #525252;

    .pdf-canvas {
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
      background: white;
    }
  }
}
</style>
```

## 五、核心问题解决

### 5.1 CORS跨域问题

**问题**：前端直接访问OSS URL时被浏览器CORS策略阻止

```
Access to fetch at 'https://xxx.oss-cn-beijing.aliyuncs.com/...' 
from origin 'http://localhost:3001' has been blocked by CORS policy
```

**解决方案**：后端代理
1. 前端不直接访问OSS，而是访问后端代理接口
2. 后端从OSS下载文件
3. 后端设置CORS响应头: `Access-Control-Allow-Origin: *`
4. 后端将文件流返回给前端

### 5.2 Vue响应式导致PDF.js私有字段访问错误

**问题**：Vue 3使用Proxy包装对象，导致PDF.js无法访问私有字段

```
TypeError: Cannot read from private field
    at Proxy.getPage (api.js:2955:23)
```

**解决方案**：使用 `markRaw` 和 `toRaw`
1. **loadPdf中**: `pdfDoc.value = markRaw(pdf)` - 防止被包装
2. **renderPage中**: `const rawPdf = toRaw(pdf)` - 提取原始对象

### 5.3 认证问题

**问题**：PDF.js的fetch请求不自动携带Authorization header

**解决方案**：手动添加header
```typescript
const response = await fetch(fullUrl, {
  method: 'GET',
  headers: {
    'Authorization': userStore.token ? `Bearer ${userStore.token}` : ''
  }
})
```

### 5.4 OSS响应头设置问题

**问题**：在生成临时URL时设置响应头会报错

```
InvalidRequest Can not override response header on content-type
```

**解决方案**：在文件上传时设置
- 上传时通过 `ObjectMetadata` 设置 `Content-Type` 和 `Content-Disposition`
- URL生成时不再设置响应头

## 六、最佳实践

### 6.1 安全性
- 后端代理接口需要认证保护
- OSS临时URL设置合理的过期时间（如60秒）
- 验证文件类型和大小

### 6.2 性能优化
- 使用流式传输，避免一次性加载大文件到内存
- 设置合理的缓冲区大小（8KB）
- PDF.js使用worker进行异步渲染

### 6.3 用户体验
- 提供加载状态指示
- 支持翻页、缩放操作
- 提供下载和新窗口打开选项
- 错误提示友好

### 6.4 资源管理
- 及时关闭输入输出流
- 断开HTTP连接
- 取消未完成的渲染任务

## 七、调试技巧

### 7.1 后端日志
```java
log.info("收到文件代理请求，ossPath: {}", ossPath);
log.info("OSS文件URL：{}", fileUrl);
log.info("文件代理下载成功");
```

### 7.2 前端控制台
```typescript
console.log('使用代理URL:', proxyUrl)
console.log('PDF数据加载成功，大小:', arrayBuffer.byteLength, 'bytes')
console.log('PDF加载成功，总页数:', totalPages.value)
console.log('开始渲染页面:', pageNum)
```

### 7.3 常见错误排查
1. **404错误**: 检查后端接口路径和OSS文件路径
2. **CORS错误**: 确认后端设置了正确的CORS响应头
3. **私有字段错误**: 确认使用了 `markRaw` 和 `toRaw`
4. **认证失败**: 检查Authorization header是否正确传递

## 八、总结

本方案通过**后端代理 + PDF.js渲染**的方式，成功实现了OSS存储的PDF文件在前端的预览功能，解决了CORS跨域和Vue响应式兼容性问题。整个方案具有良好的安全性、性能和用户体验。

**核心要点**：
1. 后端代理解决CORS问题
2. markRaw/toRaw解决Vue响应式问题
3. 流式传输优化性能
4. 完善的错误处理和日志记录

