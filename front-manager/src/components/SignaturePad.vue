<template>
  <div class="signature-pad-container">
    <div class="signature-canvas-wrapper">
      <canvas
        ref="canvasRef"
        class="signature-canvas"
        @mousedown="startDrawing"
        @mousemove="draw"
        @mouseup="stopDrawing"
        @mouseleave="stopDrawing"
        @touchstart="handleTouchStart"
        @touchmove="handleTouchMove"
        @touchend="stopDrawing"
      ></canvas>
      <div v-if="!hasSignature" class="placeholder">请在此处签名</div>
    </div>
    <div v-if="showButtons !== false" class="signature-actions">
      <el-button @click="clear" size="small">清除</el-button>
      <el-button type="primary" @click="confirm" size="small" :disabled="!hasSignature">
        确认签名
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  showButtons?: boolean
}>()

const emit = defineEmits<{
  confirm: [signature: string]
}>()

const canvasRef = ref<HTMLCanvasElement>()
const ctx = ref<CanvasRenderingContext2D | null>(null)
const isDrawing = ref(false)
const hasSignature = ref(false)
const lastX = ref(0)
const lastY = ref(0)

// 初始化画布
const initCanvas = () => {
  if (!canvasRef.value) return
  
  const canvas = canvasRef.value
  const container = canvas.parentElement
  
  if (!container) return
  
  // 获取容器的实际尺寸
  const rect = container.getBoundingClientRect()
  const width = rect.width || container.clientWidth
  const height = rect.height || container.clientHeight
  
  console.log('初始化画布，容器尺寸:', { width, height })
  
  // 设置Canvas的实际像素尺寸（使用设备像素比获得更清晰的效果）
  const dpr = window.devicePixelRatio || 1
  canvas.width = width * dpr
  canvas.height = height * dpr
  
  // 缩放画布上下文以匹配设备像素比
  ctx.value = canvas.getContext('2d')
  
  if (ctx.value) {
    ctx.value.scale(dpr, dpr)
    
    // 设置白色背景
    ctx.value.fillStyle = '#FFFFFF'
    ctx.value.fillRect(0, 0, width, height)
    
    // 设置画笔样式
    ctx.value.strokeStyle = '#000000'
    ctx.value.lineWidth = 2
    ctx.value.lineCap = 'round'
    ctx.value.lineJoin = 'round'
    
    console.log('画布初始化完成，实际尺寸:', canvas.width, 'x', canvas.height)
  }
}

// 获取坐标（相对于Canvas的CSS尺寸，不考虑设备像素比）
const getPosition = (e: MouseEvent | TouchEvent) => {
  if (!canvasRef.value) return { x: 0, y: 0 }
  
  const rect = canvasRef.value.getBoundingClientRect()
  
  if (e instanceof MouseEvent) {
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    return { x, y }
  } else {
    const touch = e.touches[0]
    const x = touch.clientX - rect.left
    const y = touch.clientY - rect.top
    return { x, y }
  }
}

// 开始绘制
const startDrawing = (e: MouseEvent) => {
  e.preventDefault()
  isDrawing.value = true
  const pos = getPosition(e)
  lastX.value = pos.x
  lastY.value = pos.y
}

// 触摸开始
const handleTouchStart = (e: TouchEvent) => {
  e.preventDefault()
  isDrawing.value = true
  const pos = getPosition(e)
  lastX.value = pos.x
  lastY.value = pos.y
}

// 绘制
const draw = (e: MouseEvent) => {
  if (!isDrawing.value || !ctx.value) return
  
  e.preventDefault()
  const pos = getPosition(e)
  
  console.log('绘制线条:', { from: { x: lastX.value, y: lastY.value }, to: pos })
  
  ctx.value.beginPath()
  ctx.value.moveTo(lastX.value, lastY.value)
  ctx.value.lineTo(pos.x, pos.y)
  ctx.value.stroke()
  
  lastX.value = pos.x
  lastY.value = pos.y
  hasSignature.value = true
}

// 触摸移动
const handleTouchMove = (e: TouchEvent) => {
  if (!isDrawing.value || !ctx.value) return
  
  e.preventDefault()
  const pos = getPosition(e)
  
  ctx.value.beginPath()
  ctx.value.moveTo(lastX.value, lastY.value)
  ctx.value.lineTo(pos.x, pos.y)
  ctx.value.stroke()
  
  lastX.value = pos.x
  lastY.value = pos.y
  hasSignature.value = true
}

// 停止绘制
const stopDrawing = () => {
  isDrawing.value = false
}

// 清除签名
const clear = () => {
  if (!canvasRef.value || !ctx.value) return
  
  const rect = canvasRef.value.getBoundingClientRect()
  const width = rect.width
  const height = rect.height
  
  // 清除画布并重新填充白色背景
  ctx.value.clearRect(0, 0, width, height)
  ctx.value.fillStyle = '#FFFFFF'
  ctx.value.fillRect(0, 0, width, height)
  
  // 恢复画笔样式
  ctx.value.strokeStyle = '#000000'
  ctx.value.lineWidth = 2
  ctx.value.lineCap = 'round'
  ctx.value.lineJoin = 'round'
  
  hasSignature.value = false
  console.log('签名已清除')
}

// 确认签名
const confirm = () => {
  if (!canvasRef.value || !hasSignature.value) {
    ElMessage.warning('请先签名')
    return
  }
  
  // 转换为Base64
  const dataUrl = canvasRef.value.toDataURL('image/png')
  // 去除 Data URL 前缀，只保留纯 Base64 数据
  const signatureBase64 = dataUrl.replace(/^data:image\/\w+;base64,/, '')
  console.log('签名已确认，Base64长度:', signatureBase64.length)
  emit('confirm', signatureBase64)
}

// 窗口大小改变时重新初始化画布
const handleResize = () => {
  if (hasSignature.value) {
    ElMessage.warning('调整窗口大小会清除签名，请重新签名')
    hasSignature.value = false
  }
  // 延迟一点以确保容器尺寸已经更新
  setTimeout(() => {
    initCanvas()
  }, 100)
}

// 检查是否有签名
const checkHasSignature = () => {
  return hasSignature.value
}

// 获取签名Base64（不触发emit）
const getSignature = () => {
  if (!canvasRef.value || !hasSignature.value) {
    return null
  }
  // 转换为Base64并去除 Data URL 前缀
  const dataUrl = canvasRef.value.toDataURL('image/png')
  return dataUrl.replace(/^data:image\/\w+;base64,/, '')
}

// 暴露方法给父组件
defineExpose({
  clear,
  confirm,
  checkHasSignature,
  getSignature
})

onMounted(() => {
  // 延迟初始化以确保DOM已完全渲染
  setTimeout(() => {
    initCanvas()
  }, 100)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped lang="scss">
.signature-pad-container {
  width: 100%;
}

.signature-canvas-wrapper {
  position: relative;
  width: 100%;
  height: 200px;
  border: 2px dashed #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  margin-bottom: 16px;
  
  &:hover {
    border-color: #409eff;
  }
}

.signature-canvas {
  width: 100%;
  height: 100%;
  cursor: crosshair;
  display: block;
}

.placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #c0c4cc;
  font-size: 14px;
  pointer-events: none;
  user-select: none;
}

.signature-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

