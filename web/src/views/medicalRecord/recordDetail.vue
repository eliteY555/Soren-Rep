<template>
  <div class="record-detail-container">
    <el-card class="record-card" shadow="hover">
      <div slot="header" class="card-header">
        <div class="header-left">
          <i class="el-icon-back" @click="goBack" style="cursor: pointer;margin-right: 10px;"></i>
          <span class="header-title">病历详情</span>
        </div>
        <div class="header-status">
          <el-tag v-if="record.status == 0" size="small" type="info">待诊断</el-tag>
          <el-tag v-else-if="record.status == 1" size="small" type="warning">已查看</el-tag>
          <el-tag v-else-if="record.status == 2" size="small" type="success">已诊断</el-tag>
          <el-tag v-else-if="record.status == 3" size="small" type="primary">诊断已结束</el-tag>
        </div>
      </div>
      
      <!-- 内容区域 -->
      <div class="record-content">
        <!-- 左侧：基本信息 -->
        <div class="info-section">
          <div class="section-title">
            <i class="el-icon-user"></i>
            <span>患者基本信息</span>
          </div>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">患者姓名：</span>
              <span class="value">{{ record.patientName }}</span>
            </div>
            <div class="info-item">
              <span class="label">患者年龄：</span>
              <span class="value">{{ record.age }}</span>
            </div>
            <div class="info-item">
              <span class="label">联系方式：</span>
              <span class="value">{{ record.phone }}</span>
            </div>
            <div class="info-item">
              <span class="label">所属科室：</span>
              <span class="value">{{ record.departmentName }}</span>
            </div>
            <div class="info-item">
              <span class="label">诊治医生：</span>
              <span class="value">{{ record.doctorName }}</span>
            </div>
          </div>
          
          <!-- 病情描述 -->
          <div class="section-title mt-20">
            <i class="el-icon-document-checked"></i>
            <span>病情描述</span>
          </div>
          <div class="description-box">
            <div class="info-item full-width">
              <span class="label">主诉：</span>
              <span class="value">{{ record.description }}</span>
            </div>
            <div class="info-item full-width">
              <span class="label">既往史：</span>
              <span class="value">{{ record.oldHistory || '无' }}</span>
            </div>
            <div class="info-item full-width">
              <span class="label">过敏史：</span>
              <span class="value">{{ record.allergiesHistory || '无' }}</span>
            </div>
            <div class="info-item full-width">
              <span class="label">生活习惯：</span>
              <span class="value">{{ record.habits || '无' }}</span>
            </div>
          </div>
        </div>
        
        <!-- 分隔线 -->
        <div class="divider">
          <div class="divider-line"></div>
        </div>
        
        <!-- 右侧：诊断信息 -->
        <div class="diagnosis-section">
          <div class="section-title">
            <i class="el-icon-first-aid-kit"></i>
            <span>诊断信息</span>
          </div>
          
          <!-- 有诊断信息时显示 -->
          <div v-if="record.diagnostic != null" class="diagnosis-content">
            <div class="diagnosis-box">
              <div class="diagnosis-header">诊断结果</div>
              <div class="diagnosis-body">{{ record.diagnostic.result }}</div>
            </div>
            
            <div class="diagnosis-box">
              <div class="diagnosis-header">药方</div>
              <div class="diagnosis-body">{{ record.diagnostic.prescription }}</div>
            </div>
            
            <div class="diagnosis-box">
              <div class="diagnosis-header">医嘱</div>
              <div class="diagnosis-body">{{ record.diagnostic.orders }}</div>
            </div>
          </div>
          
          <!-- 无诊断信息时显示 -->
          <div v-else class="no-diagnosis">
            <el-empty description="医生还没诊断,请耐心等待">
              <el-button type="primary" size="small" @click="notice">
                <i class="el-icon-bell"></i> 通知医生
              </el-button>
            </el-empty>
          </div>
        </div>
      </div>
      
      <!-- 底部按钮 -->
      <div class="action-footer">
        <el-button v-if="record.status != 3" type="primary" @click="endDialog" icon="el-icon-check">结束诊断</el-button>
        <el-button @click="comment" icon="el-icon-chat-line-round">对此诊断有疑问？</el-button>
      </div>
    </el-card>

    <!-- 评论界面 - 保持原来的实现 -->
    <el-drawer title="评论详情" :visible.sync="drawer">
      <h3 style="text-align: center; margin-bottom: 20px;">提出你的疑问吧</h3>
      <!--评论填写-->
      <el-form :model="commentForm">
        <el-form-item>
          <el-input
            type="textarea"
            style="width: 95%; margin-left: 10px"
            v-model="commentForm.content"
            placeholder="发表交流"
            autocomplete="off"
          ></el-input>
          <el-button
            type="primary"
            round
            style="float: right; margin-top: 15px; margin-right: 10px"
            @click="submitComment"
            >评论</el-button
          >
        </el-form-item>
      </el-form>
      <el-divider></el-divider>
      <!-- 评论列表 -->
      <div v-if="commentsList.length > 0">
        <div v-for="comment in commentsList" :key="comment.commentId">
          <div class="comment-item">
            <div>
              <i class="el-icon-user"></i>
              <span style="color: #9f9f9f;margin-left: 2px;">{{ comment.role == 1 ? comment.username + "医生" : comment.username }}</span>
            </div>
            <div style="margin-left: 16px;margin-bottom: 5px;">{{ comment.content }}</div>
            
            <div style="margin-left: 16px; display: flex;gap: 10px;align-items: baseline;color: #9f9f9f;">
              <div style="font-size: 12px;">{{ comment.createTime.split('T')[0] }}</div>
              <div style="font-size: 12px;cursor: pointer;" @click="showReply(comment)">回复</div>
              <!-- 只能删除自己的评论 -->
              <i v-if="comment.userId == userInfo.userId" class="el-icon-delete" style="font-size: 12px;cursor: pointer;" @click="deleteCommentById(comment)"></i>
            </div>
            <!-- 显示回复 -->
            <div v-if="comment.replies" style="margin-left: 16px;">
              <div v-if="!comment.showAllReplies && comment.replies.length > 1">
                <div v-for="reply in comment.replies.slice(0, 1)" :key="reply.replyId">
                  <div>
                    <i class="el-icon-user"></i>
                    <span style="color: #9f9f9f;margin-left: 2px;">{{ reply.username }} 回复 {{ reply.replyedName }}</span>
                  </div>
                  <div style="margin-left: 16px;margin-bottom: 5px;">{{ reply.content }}</div>
                  <div style="margin-left: 16px; display: flex;gap: 10px;align-items: baseline;color: #9f9f9f;">
                    <div style="font-size: 12px;">{{ reply.createTime.split('T')[0] }}</div>
                    <div style="font-size: 12px;cursor: pointer;" @click="showReply(comment)">回复</div>
                    <!-- 只能删除自己的评论 -->
                    <i v-if="reply.userId == userInfo.userId" class="el-icon-delete" style="font-size: 12px;cursor: pointer;" @click="deleteCommentById(reply)"></i>
                  </div>
                </div>
                <div style="margin-left: 16px; font-size: 12px; cursor: pointer;" @click="toggleReplies(comment)">
                  点击查看全部回复
                </div>
              </div>
              <div v-else>
                <div v-for="reply in comment.replies" :key="reply.replyId">
                  <div>
                    <i class="el-icon-user"></i>
                    <span style="color: #9f9f9f;margin-left: 2px;">{{ reply.username }} 回复 {{ reply.replyedName }}</span>
                  </div>
                  <div style="margin-left: 16px;margin-bottom: 5px;">{{ reply.content }}</div>
                  <div style="margin-left: 16px; display: flex;gap: 10px;align-items: baseline;color: #9f9f9f;">
                    <div style="font-size: 12px;">{{ reply.createTime.split('T')[0] }}</div>
                    <div style="font-size: 12px;cursor: pointer;" @click="showReply(comment)">回复</div>
                    <!-- 只能删除自己的评论 -->
                    <i v-if="reply.userId == userInfo.userId" class="el-icon-delete" style="font-size: 12px;cursor: pointer;" @click="deleteCommentById(reply)"></i>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <el-divider></el-divider>
        </div>
      </div>
      <div v-if="commentsList.length == 0">
        <el-empty description="暂无评论"></el-empty>
      </div>
    </el-drawer>
    
    <!-- 结束诊断对话框 - 美化 -->
    <el-dialog title="结束诊断" :visible.sync="endDialogVisible" width="30%" custom-class="end-dialog">
      <div class="end-dialog-content">
        <div class="dialog-icon">
          <i class="el-icon-check"></i>
        </div>
        <div class="dialog-message">
          <p>诊断已完成，感谢您的信任！</p>
          <p>恭喜您康复！祝您身体健康！</p>
        </div>
      </div>
      
      <div class="rate-container">
        <div class="rate-title">给此次治疗做出您的评价：</div>
        <el-rate
          v-model="score"
          show-text
          text-color="#ff9900"
          score-template="{value}分"
          :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
          :texts="['很差', '较差', '一般', '较好', '很好']"
          style="justify-content: center; font-size: 24px;"
        >
        </el-rate>
      </div>
      
      <span slot="footer" class="dialog-footer">
        <el-button @click="cancelEndDialog">取 消</el-button>
        <el-button type="primary" @click="confirmEndDialog">确认并提交评分</el-button>
      </span>
    </el-dialog>
    
    <el-dialog title="回复评论" :visible.sync="replyShow" width="30%">
      <el-form :model="replyForm">
          <el-form-item>
            <el-input
              type="textarea"
              style="width: 95%; margin-left: 10px;"
              v-model="replyForm.content"
              :placeholder="'回复：' + replyForm.replyedName"
              autocomplete="off"
            ></el-input>
          </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button @click="replyShow = false">取 消</el-button>
          <el-button type="primary" @click="submitReply">回复</el-button>
        </span>
    </el-dialog>
  </div>
</template>

<script>
import { getRecordInfo, updateRecord } from '@/api/record';
import { publishComment, getCommentList, replyComment, deleteComment, deleteReply } from '@/api/comment';
export default {
  components: {},
  data() {
    return {
      recordId: parseInt(localStorage.getItem('recordId')),
      record: {},
      diagnosis: null,
      score: 5, // 默认5分
      drawer: false,
      endDialogVisible: false, // 结束诊断对话框显示状态
      commentForm: {
        recordId: '', // 评论病历的ID
        userId: '',
        role: 0,
        content: '',
        username: ''
      },
      replyForm: {
        commentId: '', // 回复评论的ID
        userId: '',
        role: 0, // 评论的用户角色
        content: '',
        replyedName: '',
        username: ''
      },
      commentsList: [],
      replyShow: false,
    };
  },
  computed: {
    userInfo() {
      return this.$store.state.user.userInfo;
    },
    currentRecordId() {
      return parseInt(localStorage.getItem('recordId'));
    }
  },
  watch: {
    currentRecordId: {
      handler(newId, oldId) {
        if (newId && newId !== oldId) {
          this.recordId = newId;
          this.loadRecordData();
        }
      },
      immediate: true
    }
  },
  async mounted() {
    await this.loadRecordData();
    
    this.$router.afterEach(() => {
      const newRecordId = parseInt(localStorage.getItem('recordId'));
      if (newRecordId && newRecordId !== this.recordId) {
        this.recordId = newRecordId;
        this.loadRecordData();
      }
    });
  },
  methods: {
    async goBack() {
      // 发出刷新事件，通知列表页刷新数据
      this.$bus.$emit('refresh-record-list');
      
      // 返回上一级
      this.$router.go(-1);
      localStorage.removeItem('recordId');
    },
    async fetchComments() {
      try {
        const result = await getCommentList(this.recordId) || [];
      this.commentsList = result.map(comment => ({
        ...comment,
        showAllReplies: false // 默认不显示全部回复
      }));
      } catch (error) {
        console.error('获取评论列表失败:', error);
        this.commentsList = [];
      }
    },
    endDialog() {
      // 显示结束诊断对话框
      this.endDialogVisible = true;
    },
    
    cancelEndDialog() {
      this.endDialogVisible = false;
    },
    
    async confirmEndDialog() {
      try {
        // 更新状态为诊断已结束并提交评分
        await updateRecord({
          recordId: this.recordId,
          status: 3,
          score: this.score
        });
        
        // 更新本地记录状态
        this.record.status = 3;
        this.record.score = this.score;
        
        this.$message.success("感谢您的评价!");
        
        // 关闭对话框
        this.endDialogVisible = false;
        
        // 延迟一秒后自动返回上一页
        setTimeout(() => {
          this.goBack();
        }, 1000);
      } catch (error) {
        console.error('提交评分失败:', error);
        this.$message.error('评分提交失败，请稍后再试');
      }
    },
    
    async comment() {
      await this.fetchComments()
      this.drawer = true;
    },

    async notice() {
      await publishComment({...this.commentForm, content: `请${this.record.doctorName}及时查看病历`})
      this.$message.success("通知成功!")
      await this.fetchComments()
    },
    
    async submitComment() {
      await publishComment(this.commentForm)
      this.$message.success("发送成功!")
      this.commentForm.content = ''
      await this.fetchComments()
    },

    showReply(comment) {
      this.replyShow = !this.replyShow;
      if (this.replyShow) {
        this.replyForm.commentId = comment.commentId
        this.replyForm.replyedName = comment.username
      } else {
        this.replyForm.commentId = ''
        this.replyForm.replyedName = ''
      }
    },

    async submitReply() {
      await replyComment(this.replyForm)
      this.$message.success("回复成功!")
      this.replyForm.content = ''
      this.replyForm.username = ''
      this.replyForm.commentId = ''
      this.replyShow = false;
      await this.fetchComments()
    },
    toggleReplies(comment) {
      comment.showAllReplies = !comment.showAllReplies;
    },

    deleteCommentById(comment) {
      this.$confirm('是否删除该评论?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        if(comment.recordId) {
          // 删除评论
          await deleteComment(comment.commentId)
        } else {
          // 删除回复
          await deleteReply(comment.replyId)
        }
        this.$message.success("删除成功!")
        await this.fetchComments()
      });
    },
    async loadRecordData() {
      try {
        // 清除之前的数据
        this.record = {};
        this.commentsList = [];
        
        // 确保有有效的recordId
        if (!this.recordId) {
          this.recordId = parseInt(localStorage.getItem('recordId'));
          if (!this.recordId) {
            this.$message.warning('未找到病历ID');
            return;
          }
        }
        
        // 获取病历详情前先取消所有进行中的请求
        // 这样可以避免在路由切换时出现请求取消错误
        const { cancelAllRequests } = require('@/utils/request');
        cancelAllRequests();
        
        // 添加短暂延迟，确保之前的请求被完全取消
        await new Promise(resolve => setTimeout(resolve, 100));
        
        // 获取病历详情
        this.record = await getRecordInfo(this.recordId) || {};
        
        // 如果获取到了病历信息，更新页面标题
        if (this.record && this.record.patientName) {
          document.title = `诊易通 - ${this.record.patientName}的病历`;
        }
        
        // 设置评论表单数据
        this.commentForm.recordId = this.recordId;
        this.commentForm.userId = this.userInfo?.userId || '';
        this.commentForm.username = this.userInfo?.username || '';
        this.commentForm.role = this.userInfo?.role || 0;
        
        // 设置回复表单数据
        this.replyForm.userId = this.userInfo?.userId || '';
        this.replyForm.username = this.userInfo?.username || '';
        this.replyForm.role = this.userInfo?.role || 0;
        
        // 如果是医生角色，自动打开评论区
        if (this.userInfo?.role === 1) {
          await this.comment();
        }
      } catch (error) {
        // 忽略取消请求的错误
        if (error.name === 'CanceledError' || error.name === 'AbortError') {
          console.log('请求已被取消，这是正常行为');
          return;
        }
        console.error('获取病历详情失败:', error);
        this.$message.error('获取病历详情失败，请稍后再试');
      }
    },
  },
};
</script>
<style scoped lang="scss">
.record-detail-container {
  padding: 10px;
  background-color: #f9f7f4;
  min-height: 100%;
}

.record-card {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05) !important;
  background-image: linear-gradient(to bottom, #ffffff, #f9f7f4);
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(to right, #a8824a, #d3b17d);
    border-radius: 8px 8px 0 0;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee8dd;
  padding-bottom: 10px;
  
  .header-left {
    display: flex;
    align-items: center;
    
    .header-title {
      font-size: 18px;
      font-weight: 600;
      color: #60503c;
    }
  }
}

.record-content {
  display: flex;
  margin: 15px 0;
}

.section-title {
  margin-bottom: 15px;
  font-size: 16px;
  font-weight: 600;
  color: #60503c;
  display: flex;
  align-items: center;
  
  i {
    margin-right: 8px;
    color: #a8824a;
    font-size: 18px;
  }
}

.mt-20 {
  margin-top: 20px;
}

.info-section {
  flex: 1;
  padding: 10px;
}

.divider {
  width: 1px;
  margin: 0 20px;
  display: flex;
  justify-content: center;
  
  .divider-line {
    width: 1px;
    background: linear-gradient(to bottom, transparent, #d3b17d, transparent);
    height: 100%;
  }
}

.diagnosis-section {
  flex: 1;
  padding: 10px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  background-color: #f9f7f4;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #eee8dd;
}

.info-item {
  display: flex;
  
  &.full-width {
    flex-direction: column;
    margin-bottom: 10px;
    
    .label {
      margin-bottom: 5px;
    }
  }
  
  .label {
    color: #60503c;
    font-weight: 600;
    margin-right: 5px;
    min-width: 80px;
  }
  
  .value {
    color: #333;
    flex: 1;
  }
}

.description-box {
  background-color: #f9f7f4;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #eee8dd;
}

.diagnosis-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.diagnosis-box {
  border: 1px solid #eee8dd;
  border-radius: 8px;
  overflow: hidden;
  
  .diagnosis-header {
    background-color: #f3ece2;
    padding: 10px 15px;
    font-weight: 600;
    color: #60503c;
    border-bottom: 1px solid #eee8dd;
  }
  
  .diagnosis-body {
    padding: 15px;
    background-color: #fff;
    min-height: 60px;
    line-height: 1.6;
  }
}

.no-diagnosis {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f9f7f4;
  border-radius: 8px;
  padding: 30px;
  border: 1px dashed #d3b17d;
}

.action-footer {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee8dd;
}

// 结束诊断对话框样式
.end-dialog-content {
  text-align: center;
  margin-bottom: 20px;
  
  .dialog-icon {
    font-size: 48px;
    color: #67c23a;
    margin-bottom: 15px;
  }
  
  .dialog-message {
    p {
      margin: 5px 0;
      font-size: 16px;
    }
  }
}

.rate-container {
  margin: 20px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: #f9f7f4;
  padding: 15px;
  border-radius: 8px;
  
  .rate-title {
    text-align: center;
    font-size: 16px;
    margin-bottom: 15px;
    color: #60503c;
  }
}

// 覆盖Element UI的样式
:deep(.el-card__header) {
  padding: 15px 20px;
}

:deep(.el-card__body) {
  padding: 15px 20px;
}

:deep(.el-button--primary) {
  background-color: #a8824a;
  border-color: #a8824a;
  
  &:hover, &:focus {
    background-color: #c09b5e;
    border-color: #c09b5e;
  }
}

:deep(.el-textarea__inner) {
  background-color: #f5f5f5;
}

.comment-item {
  padding: 0 5px;
}

.reply-container {
  margin-top: 8px;
}

// 添加响应式布局
@media screen and (max-width: 768px) {
  .record-content {
    flex-direction: column;
  }
  
  .divider {
    width: 100%;
    height: 1px;
    margin: 20px 0;
    
    .divider-line {
      width: 80%;
      height: 1px;
      background: linear-gradient(to right, transparent, #d3b17d, transparent);
    }
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
