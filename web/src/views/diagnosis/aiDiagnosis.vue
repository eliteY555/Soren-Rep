<template>
  <div class="app-container">
    <div class="card-container">
      <div class="card-header">智能问诊</div>
      <div class="card-body">
        <div class="chat-container" ref="chatContainer">
          <div class="chat-messages" ref="chatMessages">
            <div class="message system-message">
              <div class="message-content">
                您好，我是智能问诊助手"小易"。我可以帮您提交病例、解答健康问题、提供就诊建议。请描述您的症状或需求，我将为您提供专业的辅助诊疗服务。
              </div>
            </div>
            <div v-for="(message, index) in messages" :key="index" 
                 :class="['message', message.type === 'user' ? 'user-message' : 'ai-message']">
              <div class="message-content" v-html="formatMessage(message.content)"></div>
            </div>
            <div v-if="loading" class="message ai-message">
              <div class="message-content loading-indicator">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </div>
            </div>
          </div>
        </div>
        <div class="input-container">
          <el-input
            type="textarea"
            :rows="2"
            placeholder="请详细描述您的症状或需求..."
            v-model="inputMessage"
            :disabled="loading"
            @keyup.ctrl.enter.native="sendMessage"
            resize="none"
          ></el-input>
          <div class="button-container">
            <span class="shortcut-tip">按Ctrl+Enter快速发送</span>
            <el-button 
              type="primary" 
              :loading="loading"
              :disabled="!inputMessage.trim()" 
              @click="sendMessage">发送</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'AiDiagnosis',
  data() {
    return {
      messages: [],
      inputMessage: '',
      loading: false,
      memoryId: null,
      controller: null,
      isStreaming: false
    };
  },
  computed: {
    userInfo() {
      return this.$store.state.user.userInfo;
    }
  },
  mounted() {
    this.initMemoryId();
    this.scrollToBottom();
    // 监听窗口大小变化，调整布局
    window.addEventListener('resize', this.adjustLayout);
    this.adjustLayout();
  },
  beforeDestroy() {
    // 移除事件监听
    window.removeEventListener('resize', this.adjustLayout);
  },
  methods: {
    adjustLayout() {
      // 动态调整聊天容器高度
      this.$nextTick(() => {
        this.scrollToBottom();
      });
    },
    initMemoryId() {
      // 使用用户ID作为memoryId，确保每个用户会话唯一
      if (this.userInfo && this.userInfo.userId) {
        this.memoryId = this.userInfo.userId;
        console.log('初始化memoryId:', this.memoryId);
      } else {
        console.error('无法获取用户ID');
        this.$message.error('登录信息异常，请重新登录');
      }
    },
    formatMessage(text) {
      if (!text) return '';
      
      // 将换行符转换为HTML换行
      return text.replace(/\n/g, '<br>');
    },
    async sendMessage() {
      const message = this.inputMessage.trim();
      if (!message || this.loading) return;
      
      // 添加用户消息到聊天记录
      this.messages.push({
        type: 'user',
        content: message
      });
      
      this.inputMessage = '';
      this.loading = true;
      
      // 滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom();
      });
      
      try {
        // 如果存在之前的请求，取消它
        if (this.controller) {
          this.controller.abort();
        }
        
        // 创建新的AbortController
        this.controller = new AbortController();
        
        // 添加AI消息占位
        const messageIndex = this.messages.length;
        this.messages.push({
          type: 'ai',
          content: ''
        });
        
        // 发送请求到后端
        const response = await fetch('/api/agent/chat', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            memoryId: this.memoryId,
            message: message
          }),
          signal: this.controller.signal
        });
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        // 处理流式响应
        const reader = response.body.getReader();
        const decoder = new TextDecoder("utf-8");
        
        // 设置流式输出状态
        this.isStreaming = true;
        
        // 读取流数据
        while (true) {
          const { done, value } = await reader.read();
          
          if (done) {
            break;
          }
          
          // 解码当前块
          const chunk = decoder.decode(value, { stream: true });
          
          // 逐字符添加到消息中，实现真正的流式输出
          for (let i = 0; i < chunk.length; i++) {
            const currentChar = chunk[i];
            // 将当前字符添加到现有消息
            this.$set(this.messages[messageIndex], 'content', 
              this.messages[messageIndex].content + currentChar);
            
            // 每添加几个字符就滚动一次，保持视图跟随
            if (i % 5 === 0) {
              this.scrollToBottomSmooth();
            }
            
            // 根据字符类型添加不同的延迟，使输出更自然
            let delay = 30; // 基础延迟，调整为30ms
            
            // 在标点符号后添加更长的停顿
            if (['.', '。', '!', '！', '?', '？', ';', '；', ',', '，', ':', '：'].includes(currentChar)) {
              if (['.', '。', '!', '！', '?', '？'].includes(currentChar)) {
                delay = 300; // 句号、问号、感叹号后停顿300ms
              } else {
                delay = 150; // 逗号、分号、冒号后停顿150ms
              }
            }
            
            // 添加随机波动，使输出更自然
            delay += Math.random() * 20;
            
            // 等待延迟时间
            await new Promise(resolve => setTimeout(resolve, delay));
          }
        }
        
        // 最后再滚动一次，确保显示完整内容
        this.scrollToBottom();
      } catch (error) {
        // 忽略AbortError
        if (error.name === 'AbortError') {
          console.log('请求已取消');
          return;
        }
        
        console.error('发送消息错误:', error);
        this.$message.error('发送消息失败，请稍后再试');
      } finally {
        this.loading = false;
        this.controller = null;
        this.isStreaming = false;
      }
    },
    scrollToBottom() {
      if (this.$refs.chatContainer) {
        const container = this.$refs.chatContainer;
        container.scrollTop = container.scrollHeight;
      }
    },
    // 平滑滚动到底部
    async scrollToBottomSmooth() {
      if (this.$refs.chatContainer) {
        const container = this.$refs.chatContainer;
        const isAtBottom = container.scrollHeight - container.scrollTop <= container.clientHeight + 100;
        
        // 只有当用户已经在底部或接近底部时才自动滚动
        if (isAtBottom) {
          container.scrollTop = container.scrollHeight;
        }
      }
    }
  }
};
</script>

<style lang="scss" scoped>
.app-container {
  height: calc(100vh - 110px);
  padding: 10px;
  
  .card-container {
    background-color: #f8f0e8;
    border-radius: 10px;
    overflow: hidden;
    height: 100%;
    display: flex;
    flex-direction: column;
    
    .card-header {
      background: var(--indigo-blue);
      color: white;
      padding: 8px 15px;
      display: flex;
      align-items: center;
      font-size: 16px;
      font-weight: bold;
      position: relative;
      
      &::before {
        content: "";
        display: inline-block;
        width: 4px;
        height: 16px;
        background-color: white;
        margin-right: 8px;
      }
    }
    
    .card-body {
      flex: 1;
      display: flex;
      flex-direction: column;
      padding: 10px;
      overflow: hidden;
    }
  }
}

.chat-container {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 10px;
  background-color: #fff;
  border-radius: 8px;
  padding: 10px;
  border: 1px solid #ebeef5;
  
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: rgba(0, 0, 0, 0.05);
    border-radius: 3px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: var(--indigo-blue);
    opacity: 0.7;
    border-radius: 3px;
  }
}

.chat-messages {
  display: flex;
  flex-direction: column;
}

.message {
  margin-bottom: 12px;
  max-width: 85%;
  
  &.user-message {
    align-self: flex-end;
    
    .message-content {
      background-color: #e3f2fd;
      border-radius: 15px 15px 0 15px;
      color: #333;
    }
  }
  
  &.ai-message {
    align-self: flex-start;
    
    .message-content {
      background-color: #f5f5f5;
      border-radius: 15px 15px 15px 0;
      color: #333;
    }
  }
  
  &.system-message {
    align-self: center;
    max-width: 95%;
    margin-bottom: 15px;
    
    .message-content {
      background-color: #f9f0e6;
      border-radius: 10px;
      color: #8b6f4e;
      border: 1px dashed #d4b894;
      font-size: 13px;
    }
  }
}

.message-content {
  padding: 8px 12px;
  line-height: 1.5;
  word-break: break-word;
  
  ::v-deep br {
    content: "";
    display: block;
    margin-bottom: 5px;
  }
}

.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  
  .dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background-color: #999;
    margin: 0 3px;
    animation: dot-flashing 1s infinite alternate;
    
    &:nth-child(2) {
      animation-delay: 0.2s;
    }
    
    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes dot-flashing {
  0% {
    opacity: 0.2;
  }
  100% {
    opacity: 1;
  }
}

.input-container {
  display: flex;
  flex-direction: column;
  background-color: #fff;
  border-radius: 8px;
  padding: 10px;
  border: 1px solid #ebeef5;
}

.button-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.shortcut-tip {
  font-size: 12px;
  color: #909399;
}

::v-deep .el-textarea__inner {
  resize: none;
  border-color: #d4b894;
  min-height: 60px !important;
  
  &:focus {
    border-color: var(--indigo-blue);
  }
}
</style> 