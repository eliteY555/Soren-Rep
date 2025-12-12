<template>
  <div class="records-container">
    <el-card class="records-card" v-loading="loading">
      <div class="card-header">
        <div class="header-left">
        <h2>咨询列表</h2>
          <div class="header-decoration">
            <span class="decoration-line"></span>
            <span class="decoration-dot"></span>
          </div>
        </div>
        <div class="decorative-icon">
          <img src="@/assets/images/patterns/geometric-pattern.svg" alt="装饰图案" class="decorative-pattern" />
        </div>
      </div>
      <div v-for="item in recordList" :key="item.recordId">
        <el-descriptions :column="3" border style="margin-bottom: 20px" class="record-description">
          <el-descriptions-item>
            <template slot="label">
              <i class="el-icon-user"></i>
              姓名
            </template>
            {{ item.patientName }}
          </el-descriptions-item>
          <el-descriptions-item>
            <template slot="label">
              <i class="el-icon-mobile-phone"></i>
              手机号
            </template>
            {{ item.phone }}
          </el-descriptions-item>
          <el-descriptions-item>
            <template slot="label">
              <i class="el-icon-time"></i>
              年龄
            </template>
            {{ item.age }}
          </el-descriptions-item>
          <el-descriptions-item :contentStyle="{ width: '350px' }">
            <template slot="label">
              <i class="el-icon-tickets"></i>
              病症详情
            </template>
            <span class="text-ellipsis">{{ item.description }}</span>
          </el-descriptions-item>
          <el-descriptions-item>
            <template slot="label">
              <i class="el-icon-tickets"></i>
              所属科室
            </template>
            {{ item.departmentName }}
          </el-descriptions-item>
          <el-descriptions-item>
            <template slot="label">
              <i class="el-icon-service"></i>
              诊治医生
            </template>
            {{ item.doctorName }}
          </el-descriptions-item>
          <el-descriptions-item>
            <template slot="label">
              <i class="el-icon-notebook-1"></i>
              病历状态
            </template>
            <el-tag v-if="item.status == 0" size="small" type="info">未查看</el-tag>
            <el-tag v-if="item.status == 1" size="small" type="warning">已查看</el-tag>
            <el-tag v-if="item.status == 2" size="small" type="success">已诊断</el-tag>
            <el-tag v-if="item.status == 3" size="small" type="primary">诊断已结束</el-tag>
            <el-link style="margin-left: 10px" type="primary" @click="goRecordDetail(item.recordId)"
              >点击查看详情</el-link
            >
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <el-empty v-if="recordList.length === 0" description="暂无咨询记录"></el-empty>
      
      <div class="pagination" v-if="recordList.length > 0">
      <el-pagination
        @current-change="(val) => handleQuery({ page: val })"
        @size-change="(val) => handleQuery({ pageSize: val })"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[5, 10, 20, 50, 100]"
        :current-page="tableParams.page"
        :page-size="tableParams.pageSize"
        :total="total"
      >
      </el-pagination>
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
import { getRecordList } from '@/api/record';
import { getPatientInfo } from "@/api/patient";
import { debounce } from '@/utils/request';

export default {
  components: {},
  data() {
    return {
      title: '咨询列表',
      recordList: [],
      tableParams: {
        page: 1,
        pageSize: 5,
      },
      total: 0,
      loading: false
    };
  },
  computed: {
    userInfo() {
      return this.$store.state.user.userInfo;
    },
  },
  mounted() {
    this.initData();
    
    // 监听从病历详情页返回时刷新数据
    this.$bus.$on('refresh-record-list', this.handleQuery);
  },
  beforeDestroy() {
    // 移除事件监听
    this.$bus.$off('refresh-record-list', this.handleQuery);
  },
  methods: {
    // 初始化数据
    async initData() {
      await this.handleQuery();
    },
    
    // 使用防抖处理查询
    handleQuery: debounce(async function(params = {}) {
      if (this.loading) return;
      
      try {
        this.loading = true;
        
        // 获取患者信息
        const patient = await getPatientInfo(this.userInfo?.userId);
        if (!patient || !patient.patientId) {
          console.error('未找到患者ID:', patient);
          this.recordList = [];
          this.total = 0;
          return;
        }
        
        console.log('获取到患者ID:', patient.patientId);
        
        // 查询病历记录
        this.tableParams = { 
          ...this.tableParams, 
          ...params, 
          patientId: parseInt(patient.patientId) // 确保是数字类型
        };
        console.log('查询参数:', this.tableParams);
        
        const result = await getRecordList(this.tableParams);
        console.log('查询结果:', result);
        
        if (result) {
          this.recordList = result.recordList || [];
          this.total = result.total || 0;
        }
      } catch (error) {
        // 忽略请求取消的错误
        if (error && error.name === 'RequestCancelError') {
          return;
        }
        console.error('获取病历列表失败:', error);
      } finally {
        this.loading = false;
      }
    }, 300),
    
    goRecordDetail(recordId) {
      if (!recordId) return;
      
      localStorage.setItem('recordId', recordId);
      this.$router.push({
        path: '/home/recordDetail'
      });
    }
  },
};
</script>
<style scoped lang="scss">
.records-container {
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
  
  .records-card {
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
            background: linear-gradient(135deg, var(--indigo-blue), #7d93b3);
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
            background: linear-gradient(90deg, var(--indigo-blue), var(--lotus-pink));
            border-radius: 1px;
          }
          
          .decoration-dot {
            width: 5px;
            height: 5px;
            border-radius: 50%;
            background-color: var(--indigo-blue);
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
        background: linear-gradient(135deg, var(--indigo-blue), #7d93b3);
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

.text-ellipsis {
  display: inline-block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 330px; // 根据需要调整最大宽度
}

.record-description {
  background-color: rgba(255, 255, 255, 0.6);
  border-radius: var(--border-radius);
  transition: all 0.3s;
  
  &:hover {
    box-shadow: var(--shadow-sm);
    background-color: rgba(255, 255, 255, 0.8);
  }
}

.pagination {
  margin-top: 15px;
  text-align: center;
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
  .records-container {
    padding: 10px;
    
    .records-card {
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
    }
  }
}
</style>

