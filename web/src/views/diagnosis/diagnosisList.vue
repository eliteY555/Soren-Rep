<template>
  <div class="diagnosis-container">
    <el-card class="diagnosis-card">
      <div class="card-header">
        <div class="header-left">
          <h2>诊断管理</h2>
          <div class="header-decoration">
            <span class="decoration-line"></span>
            <span class="decoration-dot"></span>
          </div>
        </div>
        <div class="decorative-icon">
          <img src="@/assets/images/patterns/geometric-pattern.svg" alt="装饰图案" class="decorative-pattern" />
        </div>
      </div>
      <el-tabs v-model="activeName" class="diagnosis-tabs">
      <el-tab-pane label="全部病历" name="all">
          <all v-if="activeName === 'all'" :activeName="activeName" @update-undiagnosed="updateUndiagnosedStatus"/>
      </el-tab-pane>
      <el-tab-pane name="noDiagnosis">
        <span slot="label">
          <el-badge :is-dot="hasUndiagnosedRecords">未诊断病历</el-badge>
        </span>
          <all v-if="activeName === 'noDiagnosis'" :activeName="activeName" @update-undiagnosed="updateUndiagnosedStatus"/>
      </el-tab-pane>
    </el-tabs>
    </el-card>
    
    <!-- 装饰元素 -->
    <div class="floating-patterns">
      <div class="floating-pattern pattern1"></div>
      <div class="floating-pattern pattern2"></div>
      <div class="floating-pattern pattern3"></div>
    </div>
  </div>
</template>

<script>
import all from "./components/all";
import { debounce, isRequestCanceled, cancelAllRequests } from '@/utils/request';
import { getDoctorInfo } from "@/api/doctor";

export default {
  components: { all },
  data() {
    return {
      activeName: "all",
      hasUndiagnosedRecords: false,
    };
  },
  computed: {
    userInfo() {
      return this.$store.state.user.userInfo;
    }
  },
  mounted() {
    // 如果用户信息已加载，则初始化数据
    if (this.userInfo && this.userInfo.userId) {
      this.initData();
    }
    
    // 监听用户信息加载事件
    this.$bus.$on('user-info-loaded', this.onUserInfoLoaded);
    
    // 监听诊断完成事件，刷新列表
    this.$bus.$on('refresh-diagnosis-list', this.refreshList);
  },
  beforeDestroy() {
    // 移除事件监听
    this.$bus.$off('user-info-loaded', this.onUserInfoLoaded);
    this.$bus.$off('refresh-diagnosis-list', this.refreshList);
    
    // 取消所有未完成的请求
    cancelAllRequests();
  },
  watch: {
    // 监听用户信息变化，当用户信息加载完成后初始化数据
    userInfo: {
      handler(newVal) {
        if (newVal && newVal.userId) {
          this.initData();
        }
      },
      immediate: true
    }
  },
  methods: {
    // 初始化数据
    async initData() {
      // 确保用户是医生角色
      if (!this.userInfo || this.userInfo.role !== 1) {
        this.$message.warning('当前用户不是医师角色，无法使用诊断功能');
        // 可能需要跳转回首页
        setTimeout(() => {
          this.$router.push('/home');
        }, 1500);
        return;
      }
      
      try {
        // 判断医生信息是否完整
        const doctorInfo = await this.getDoctorInfo();
        
        // 如果医生信息为空或无效，显示警告
        if (!doctorInfo) {
          this.$message.warning('获取医师信息失败，请稍后再试');
          return;
        }
        
        // 检查医生信息是否完整
        if (!doctorInfo.doctorId || !doctorInfo.doctorName || !doctorInfo.departmentName || !doctorInfo.hospitalName) {
          this.$message.warning('医师信息不完整，请联系管理员完善您的医师资料');
          return;
        }
        
        // 初始化诊断列表
        await this.handleQuery();
      } catch (error) {
        // 忽略请求取消的错误
        if (error && (error.name === 'CanceledError' || error.name === 'RequestCancelError' || error.code === 'ERR_CANCELED')) {
          return;
        }
        console.error('初始化诊断列表失败:', error);
        this.$message.error('加载诊断数据失败，请稍后再试');
      }
    },
    
    // 获取医生信息
    async getDoctorInfo() {
      let retryCount = 0;
      const maxRetries = 3;
      
      // 增加延迟确保用户信息已完全加载
      if (!this.userInfo || !this.userInfo.userId) {
        console.log('等待用户信息加载...');
        await new Promise(resolve => setTimeout(resolve, 300));
        // 再次检查用户信息
        if (!this.userInfo || !this.userInfo.userId) {
          console.log('用户ID仍不存在，无法获取医师信息');
          return null;
        }
      }
      
      const attemptFetch = async () => {
        try {
          console.log(`尝试获取医师信息 (userId: ${this.userInfo.userId}), 尝试次数: ${retryCount + 1}`);
          
          // 添加防抖动，避免快速多次请求
          await new Promise(resolve => setTimeout(resolve, 100 * retryCount));
          
          // 设置更长的超时时间和更多重试次数
          const doctorInfo = await getDoctorInfo(this.userInfo.userId);
          
          if (!doctorInfo) {
            throw new Error('医师信息为空');
          }
          
          console.log('成功获取到医师信息:', doctorInfo);
          return doctorInfo;
        } catch (error) {
          // 忽略请求取消的错误
          if (isRequestCanceled(error)) {
            console.log('获取医师信息请求被取消，可能是由于页面切换');
            return null;
          }
          
          console.error(`获取医师信息失败 (尝试 ${retryCount + 1}/${maxRetries}):`, error);
          
          // 如果未达到最大重试次数，则重试
          if (retryCount < maxRetries - 1) {
            retryCount++;
            const waitTime = 1000 * Math.pow(1.5, retryCount); // 指数退避策略
            console.log(`等待${waitTime/1000}秒后重试... (${retryCount}/${maxRetries - 1})`);
            await new Promise(resolve => setTimeout(resolve, waitTime));
            return attemptFetch();
          }
          
          return null;
        }
      };
      
      return attemptFetch();
    },
    
    // 使用防抖处理查询
    handleQuery: debounce(async function(params = {}) {
      try {
        console.log('开始查询诊断列表...');
        // 获取医生信息，确保有医生ID
        const doctorInfo = await this.getDoctorInfo();
        if (!doctorInfo || !doctorInfo.doctorId) {
          console.error('无法获取医生ID，无法查询诊断列表');
          return;
        }
        
        // 通知子组件刷新数据
        this.$nextTick(() => {
          // 触发子组件的刷新方法
          const allComponent = this.$children.find(child => child.$options.name === 'all');
          if (allComponent && typeof allComponent.handleQuery === 'function') {
            console.log('通知子组件刷新数据...');
            allComponent.handleQuery();
          } else {
            console.warn('未找到子组件或刷新方法');
          }
        });
      } catch (error) {
        // 忽略请求取消的错误
        if (isRequestCanceled(error)) {
          console.log('查询请求被取消');
          return;
        }
        console.error('获取诊断列表失败:', error);
        this.$message.error('获取诊断列表失败，请稍后再试');
      }
    }, 300),
    
    updateUndiagnosedStatus(hasRecords) {
      this.hasUndiagnosedRecords = hasRecords;
    },
    
    // 刷新列表数据
    refreshList() {
      if (this.userInfo && this.userInfo.userId) {
        this.initData();
      }
    },
    
    // 用户信息加载后的回调
    onUserInfoLoaded(userInfo) {
      if (userInfo && userInfo.userId) {
        // 延迟执行初始化，确保用户信息已完全加载到组件中
        setTimeout(() => {
          console.log('用户信息已加载，开始初始化数据...');
          this.initData();
        }, 300);
      }
    }
  },
};
</script>
<style scoped lang="scss">
.diagnosis-container {
  padding: 15px;
  height: 100%;
  background-image: url('@/assets/images/patterns/cloud-pattern.svg');
  background-size: 300px;
  background-repeat: repeat;
  background-position: center;
  background-attachment: fixed;
  background-opacity: 0.05;
  position: relative;
  overflow: hidden;
  
  .diagnosis-card {
    height: 100%;
    border-radius: var(--border-radius-lg);
    box-shadow: var(--shadow-md);
    background: var(--gradient-parchment);
    border-top: 3px solid var(--cinnabar-red);
    position: relative;
    z-index: 2;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    
    .card-header {
      margin-bottom: 15px;
      border-bottom: 1px solid var(--border-color);
      padding: 10px 15px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-shrink: 0;
      
      .header-left {
        display: flex;
        flex-direction: column;
        min-width: 0; /* 允许内容压缩 */
        
        h2 {
          margin: 0;
          color: var(--rhubarb-brown);
          font-size: 18px;
          font-weight: 500;
          position: relative;
          padding-left: 15px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          
          &::before {
            content: '';
            position: absolute;
            left: 0;
            top: 50%;
            transform: translateY(-50%);
            width: 4px;
            height: 18px;
            background: var(--gradient-cinnabar);
            border-radius: 2px;
          }
        }
        
        .header-decoration {
          display: flex;
          align-items: center;
          margin-top: 3px;
          margin-left: 15px;
          
          .decoration-line {
            height: 2px;
            width: 40px;
            background: var(--gradient-chrysanthemum);
            border-radius: 1px;
          }
          
          .decoration-dot {
            width: 5px;
            height: 5px;
            border-radius: 50%;
            background-color: var(--chrysanthemum-yellow);
            margin-left: 5px;
          }
        }
      }
      
      .decorative-icon {
        width: 40px;
        height: 40px;
        display: flex;
        justify-content: center;
        align-items: center;
        background: var(--gradient-herbal);
        border-radius: 50%;
        padding: 8px;
        flex-shrink: 0;
        margin-left: 15px;
        
        .decorative-pattern {
          width: 100%;
          height: 100%;
          opacity: 0.8;
          filter: brightness(2);
        }
      }
    }
    
    .diagnosis-tabs {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      
      :deep(.el-tabs__header) {
        margin-bottom: 15px;
        border-radius: var(--border-radius);
        padding: 3px;
        background-color: rgba(255, 255, 255, 0.6);
        position: relative;
        flex-shrink: 0;
        
        &::before {
          content: '';
          position: absolute;
          top: 0;
          right: 0;
          width: 60px;
          height: 60px;
          background-image: url('@/assets/images/patterns/floral-pattern.svg');
          background-size: 60px;
          background-repeat: no-repeat;
          opacity: 0.1;
          transform: rotate(45deg) translate(20px, -20px);
        }
      }
      
      :deep(.el-tabs__nav-wrap::after) {
        background-color: var(--border-color);
        height: 1px;
      }
      
      :deep(.el-tabs__item) {
        color: var(--text-secondary);
        padding: 0 20px;
        height: 36px;
        line-height: 36px;
        transition: all 0.3s;
        font-weight: 500;
        
        &.is-active {
          color: var(--cinnabar-red);
        }
        
        &:hover {
          color: var(--realgar-orange);
        }
      }
      
      :deep(.el-tabs__active-bar) {
        background: var(--gradient-cinnabar);
        height: 3px;
      }
      
      :deep(.el-tabs__content) {
        flex: 1;
        overflow: hidden;
      }
    }
  }
  
  /* 浮动装饰元素 */
  .floating-patterns {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
    z-index: 1;
    
    .floating-pattern {
      position: absolute;
      background-repeat: no-repeat;
      background-size: contain;
      opacity: 0.1;
      
      &.pattern1 {
        width: 150px;
        height: 150px;
        top: 5%;
        right: 5%;
        background-image: url('@/assets/images/patterns/floral-pattern.svg');
        animation: float 15s ease-in-out infinite;
      }
      
      &.pattern2 {
        width: 200px;
        height: 200px;
        bottom: 5%;
        left: 5%;
        background-image: url('@/assets/images/patterns/cloud-pattern.svg');
        animation: float 20s ease-in-out infinite reverse;
      }
      
      &.pattern3 {
        width: 100px;
        height: 100px;
        top: 40%;
        right: 10%;
        background-image: url('@/assets/images/patterns/geometric-pattern.svg');
        animation: float 12s ease-in-out infinite 2s;
      }
    }
  }
}

:deep(.el-badge__content.is-fixed.is-dot) {
  top: 6px;
  right: 10px;
  background-color: var(--cinnabar-red);
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-15px) rotate(5deg);
  }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .diagnosis-container {
    padding: 10px;
    
    .diagnosis-card {
      .card-header {
        padding: 10px 15px;
        
        .header-left {
          h2 {
            font-size: 18px;
            padding-left: 12px;
            
            &::before {
              width: 3px;
              height: 16px;
            }
          }
          
          .header-decoration {
            margin-left: 12px;
            
            .decoration-line {
              width: 30px;
            }
            
            .decoration-dot {
              width: 5px;
              height: 5px;
            }
          }
        }
        
        .decorative-icon {
          width: 40px;
          height: 40px;
          padding: 8px;
        }
      }
      
      .diagnosis-tabs {
        :deep(.el-tabs__item) {
          padding: 0 15px;
          font-size: 14px;
        }
      }
    }
  }
}
</style>
