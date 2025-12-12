<template>
  <div class="communicate-container">
    <el-card class="communicate-card">
      <div class="card-header">
        <div class="header-left">
          <h2>{{ userInfo ? userInfo.username : '医患交流' }}</h2>
          <div class="header-decoration">
            <span class="decoration-line"></span>
            <span class="decoration-dot"></span>
          </div>
        </div>
        <div class="decorative-icon">
          <img src="@/assets/images/patterns/geometric-pattern.svg" alt="装饰图案" class="decorative-pattern" />
        </div>
      </div>
      
      <div class="communicate-content">
        <div v-if="loading" class="loading-container">
          <el-skeleton :rows="3" animated />
          <el-skeleton :rows="3" animated style="margin-top: 20px;" />
        </div>
        
        <div v-else-if="filteredCommentList.length === 0" class="empty-container">
          <el-empty description="暂无评论信息">
            <template #image>
              <i class="el-icon-chat-line-round empty-icon"></i>
            </template>
          </el-empty>
        </div>
        
        <div v-else class="comment-list">
          <div 
            v-for="comment in filteredCommentList" 
            :key="comment.commentId"
            class="comment-card"
            @click="goRecordDetail(comment.recordId, comment.patientName)"
          >
            <div class="comment-header">
              <div class="user-info">
                <i class="el-icon-user"></i>
                <span class="username">{{ comment.patientName }}</span>
                <el-tag size="mini" type="info">患者</el-tag>
              </div>
              <div class="comment-date">{{ formatDate(comment.createTime) }}</div>
            </div>
            
            <div class="comment-content">
              <i class="el-icon-chat-dot-square"></i>
              <div class="content-text">{{ comment.content }}</div>
            </div>
            
            <div class="case-info">
              <i class="el-icon-document-copy"></i>
              <div class="case-description">
                <span class="label">病情描述：</span>
                <span class="text">{{ comment.record ? comment.record.description : '' }}</span>
              </div>
            </div>
            
            <div class="comment-footer">
              <el-button type="text" class="view-detail">
                <i class="el-icon-view"></i> 查看详情
              </el-button>
              <el-button type="text" class="reply-button">
                <i class="el-icon-s-comment"></i> 回复
              </el-button>
            </div>
          </div>
        </div>
      </div>
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
import { getCommentList } from '@/api/comment'
import { getRecordInfo } from '@/api/record'
import { getDoctorInfo } from '@/api/doctor'
import { getPatientInfo } from '@/api/patient'
import { isRequestCanceled, cancelAllRequests } from '@/utils/request'

export default {
  components: {

  },
  data () {
    return {
      filteredCommentList: [],
      loading: false,
      doctorInfo: null,
      isComponentMounted: false  // 组件挂载状态标志
    }
  },
  computed: {
    userInfo() {
      return this.$store.state.user.userInfo;
    }
  },
  async created() {
    // 在created钩子中准备加载，但不实际触发请求
    this.loading = true;
  },
  async mounted() {
    this.isComponentMounted = true; // 标记组件已挂载
    
    // 如果用户信息已加载，则立即加载评论
    if (this.userInfo && this.userInfo.userId) {
      try {
        await this.loadComments();
      } catch (error) {
        this.handleError(error);
      }
    }
    
    // 监听用户信息加载事件
    this.$bus.$on('user-info-loaded', this.onUserInfoLoaded);
    
    // 监听路由完成事件
    this.$bus.$on('route-complete', this.onRouteComplete);
    
    // 添加路由钩子，用于处理路由重新激活的情况
    this.routerGuard = this.$router.beforeEach((to, from, next) => {
      if (to.path === '/home/communicate' && from.path.startsWith('/home/')) {
        // 当从其他tab切回医患交流tab时，重新加载数据
        this.$nextTick(async () => {
          if (this.isComponentMounted && this.userInfo && this.userInfo.userId) {
            try {
              await this.loadComments();
            } catch (error) {
              this.handleError(error);
            }
          }
        });
      }
      next();
    });
    
    // 清除可能存在的患者信息，避免混淆
    localStorage.removeItem('currentPatientId');
    localStorage.removeItem('currentPatientName');
  },
  watch: {
    // 监听用户信息变化，当用户信息加载完成后加载评论
    userInfo: {
      async handler(newVal) {
        if (newVal && newVal.userId && this.isComponentMounted) {
          try {
            await this.loadComments();
          } catch (error) {
            this.handleError(error);
          }
        }
      },
      immediate: true
    }
  },
  beforeDestroy() {
    // 移除事件监听
    this.$bus.$off('user-info-loaded', this.onUserInfoLoaded);
    this.$bus.$off('route-complete', this.onRouteComplete);
    
    // 移除路由守卫
    if (this.routerGuard && typeof this.routerGuard === 'function') {
      this.routerGuard(); // 解绑路由守卫
    }
    
    // 组件销毁时取消所有请求
    cancelAllRequests();
    
    // 重置组件状态
    this.isComponentMounted = false;
  },
  activated() {
    // keep-alive组件被激活时调用
    if (this.isComponentMounted && this.userInfo && this.userInfo.userId) {
      this.$nextTick(async () => {
        try {
          await this.loadComments();
        } catch (error) {
          this.handleError(error);
        }
      });
    }
  },
  methods: {
    formatDate(dateString) {
      if (!dateString) return '';
      // 如果日期字符串包含'T'，则取'T'前面的部分
      return dateString.split('T')[0];
    },
    
    handleError(error) {
      // 如果是请求取消错误，静默处理
      if (isRequestCanceled(error)) {
        console.log('请求已被取消，这是正常行为');
        return;
      }
      console.error('加载评论失败:', error);
      this.$message.error('加载评论失败，请稍后再试');
    },
    
    async loadComments() {
      if (!this.isComponentMounted) {
        return; // 如果组件未挂载，不执行加载
      }
      
      this.loading = true;
      try {
        if (!this.userInfo || !this.userInfo.userId) {
          this.$message.warning('用户信息未加载，请重新登录');
          return;
        }
        
        // 获取医生信息
        if (!this.doctorInfo || !this.doctorInfo.doctorId) {
          this.doctorInfo = await getDoctorInfo(this.userInfo.userId);
          if (!this.doctorInfo || !this.doctorInfo.doctorId) {
            this.$message.warning('未找到医生信息');
            return;
          }
        }
        
        // 获取评论列表
        const commentList = await getCommentList();
        
        // 使用Promise.all并行获取所有记录信息
        const commentPromises = commentList.map(async (comment) => {
          try {
            const record = await getRecordInfo(comment.recordId);
            // 只过滤出当前医生的记录
            if (record && record.doctorId === this.doctorInfo.doctorId) {
              // 确保显示患者姓名而不是评论者姓名
              const patientName = record.patientName || comment.username;
              return { 
                ...comment, 
                record,
                patientName // 添加患者姓名字段
              };
            }
            return null;
          } catch (error) {
            if (isRequestCanceled(error)) {
              return null;
            }
            console.error(`获取记录 ${comment.recordId} 失败:`, error);
            return null;
          }
        });
        
        // 等待所有请求完成并过滤掉null结果
        const results = await Promise.all(commentPromises);
        this.filteredCommentList = results.filter(item => item !== null);
      } finally {
        if (this.isComponentMounted) { // 确保组件仍然挂载
          this.loading = false;
        }
      }
    },
    
    goRecordDetail(recordId, patientName) {
      // 先取消所有正在进行的请求，避免路由切换时出现请求取消错误
      cancelAllRequests();
      
      // 清除localStorage中的数据，避免混淆
      localStorage.removeItem('currentPatientId');
      localStorage.removeItem('currentPatientName');
      localStorage.removeItem('recordId');
      
      // 确保recordId被正确存储
      localStorage.setItem('recordId', recordId);
      
      // 使用nextTick确保DOM更新和请求取消后再跳转
      this.$nextTick(async () => {
        // 添加短暂延迟，确保之前的请求被完全取消
        await new Promise(resolve => setTimeout(resolve, 100));
        
        // 跳转到病历详情页
        this.$router.push({
          path: '/home/recordDetail'
        });
      });
    },
    
    // 用户信息加载后的回调
    async onUserInfoLoaded(userInfo) {
      if (userInfo && userInfo.userId && this.isComponentMounted) {
        try {
          await this.loadComments();
        } catch (error) {
          this.handleError(error);
        }
      }
    },
    
    // 路由完成后的回调
    async onRouteComplete(routeInfo) {
      if (routeInfo.to.path === '/home/communicate' && this.isComponentMounted && this.userInfo && this.userInfo.userId) {
        try {
          await this.loadComments();
        } catch (error) {
          this.handleError(error);
        }
      }
    }
  },
}
</script>

<style scoped lang='scss'>
.communicate-container {
  padding: 20px;
  height: 100%;
  background-image: url('@/assets/images/patterns/cloud-pattern.svg');
  background-size: 300px;
  background-repeat: repeat;
  background-position: center;
  background-attachment: fixed;
  background-opacity: 0.05;
  position: relative;
  overflow: hidden;
  
  .communicate-card {
    height: 100%;
    border-radius: var(--border-radius-lg);
    box-shadow: var(--shadow-md);
    background: var(--gradient-parchment);
    border-top: 3px solid var(--indigo-blue);
    position: relative;
    z-index: 2;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    
    .card-header {
      margin-bottom: 20px;
      border-bottom: 1px solid var(--border-color);
      padding: 15px 20px;
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
          font-size: 20px;
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
            height: 20px;
            background: linear-gradient(135deg, var(--indigo-blue), #7d93b3);
            border-radius: 2px;
          }
        }
        
        .header-decoration {
          display: flex;
          align-items: center;
          margin-top: 5px;
          margin-left: 15px;
          
          .decoration-line {
            height: 2px;
            width: 40px;
            background: linear-gradient(90deg, var(--indigo-blue), var(--lotus-pink));
            border-radius: 1px;
          }
          
          .decoration-dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background-color: var(--indigo-blue);
            margin-left: 5px;
          }
        }
      }
      
      .decorative-icon {
        width: 50px;
        height: 50px;
        display: flex;
        justify-content: center;
        align-items: center;
        background: linear-gradient(135deg, var(--indigo-blue), #7d93b3);
        border-radius: 50%;
        padding: 10px;
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
    
    .communicate-content {
      flex: 1;
      overflow-y: auto;
      padding: 0 20px 20px;
      
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
      
      .loading-container {
        padding: 20px 0;
      }
      
      .empty-container {
        display: flex;
        justify-content: center;
        align-items: center;
        height: 100%;
        
        .empty-icon {
          font-size: 60px;
          color: var(--indigo-blue);
          opacity: 0.5;
        }
      }
      
      .comment-list {
        display: flex;
        flex-direction: column;
        gap: 20px;
        
        .comment-card {
          background-color: rgba(255, 255, 255, 0.6);
          border-radius: var(--border-radius);
          box-shadow: var(--shadow-sm);
          padding: 15px;
          transition: all 0.3s;
          border-left: 3px solid var(--indigo-blue);
          cursor: pointer;
          
          &:hover {
            transform: translateY(-2px);
            box-shadow: var(--shadow-md);
            background-color: rgba(255, 255, 255, 0.8);
          }
          
          .comment-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 12px;
            
            .user-info {
              display: flex;
              align-items: center;
              gap: 8px;
              
              i {
                color: var(--indigo-blue);
                font-size: 16px;
              }
              
              .username {
                font-weight: 600;
                color: var(--rhubarb-brown);
              }
            }
            
            .comment-date {
              color: var(--text-secondary);
              font-size: 13px;
            }
          }
          
          .comment-content {
            display: flex;
            margin-bottom: 12px;
            
            i {
              color: var(--realgar-orange);
              margin-right: 10px;
              margin-top: 3px;
              flex-shrink: 0;
            }
            
            .content-text {
              color: var(--text-primary);
              line-height: 1.5;
              flex: 1;
            }
          }
          
          .case-info {
            display: flex;
            margin-bottom: 12px;
            background-color: rgba(248, 240, 221, 0.5);
            padding: 10px;
            border-radius: var(--border-radius);
            
            i {
              color: var(--licorice-tan);
              margin-right: 10px;
              margin-top: 3px;
              flex-shrink: 0;
            }
            
            .case-description {
              flex: 1;
              
              .label {
                color: var(--text-secondary);
                margin-right: 5px;
              }
              
              .text {
                color: var(--text-primary);
                word-break: break-word;
              }
            }
          }
          
          .comment-footer {
            display: flex;
            justify-content: flex-end;
            gap: 15px;
            margin-top: 10px;
            
            .view-detail, .reply-button {
              color: var(--indigo-blue);
              padding: 0;
              
              &:hover {
                color: var(--cinnabar-red);
              }
              
              i {
                margin-right: 5px;
              }
            }
          }
        }
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
  .communicate-container {
    padding: 10px;
    
    .communicate-card {
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
      
      .communicate-content {
        padding: 0 15px 15px;
        
        .comment-card {
          padding: 12px;
          
          .comment-header {
            flex-direction: column;
            align-items: flex-start;
            gap: 5px;
            
            .comment-date {
              align-self: flex-end;
            }
          }
          
          .comment-footer {
            justify-content: space-between;
          }
        }
      }
    }
  }
}
</style>