<template>
  <div style="height: 100%; width: 100%; display: flex; flex-direction: column; overflow: hidden;">
    <div class="filter-container">
      <el-input
        v-model="form.keyword"
        placeholder="请输入患者姓名/手机号"
        class="filter-item"
        @keyup.enter.native="handleQuery"
      >
        <i slot="prefix" class="el-icon-search"></i>
      </el-input>
      <el-button class="filter-button" type="primary" icon="el-icon-search" @click="handleQuery">
        查询
      </el-button>
      <el-button class="filter-button" icon="el-icon-refresh" @click="resetQuery">
        重置
      </el-button>
    </div>

    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="recordList"
        border
        class="medical-table"
        :header-cell-style="{ background: 'var(--ginseng-beige)', color: 'var(--rhubarb-brown)', fontWeight: '600' }"
        :row-class-name="tableRowClassName"
        size="mini"
      >
        <el-table-column label="患者信息" align="center" min-width="180">
          <template slot-scope="scope">
            <div class="patient-info">
              <div class="patient-name">
              <i class="el-icon-user"></i>
                <span>{{ scope.row.patientName }}</span>
              </div>
              <div class="patient-details">
                <span><i class="el-icon-mobile-phone"></i> {{ scope.row.phone }}</span>
                <span><i class="el-icon-time"></i> {{ scope.row.age }}岁</span>
              </div>
            </div>
            </template>
        </el-table-column>
        
        <el-table-column label="病症详情" prop="description" min-width="280">
          <template slot-scope="scope">
            <div class="symptom-info">
              <i class="el-icon-tickets"></i>
              <el-tooltip v-if="scope.row.description && scope.row.description.length > 50" :content="scope.row.description" placement="top">
                <span class="text-ellipsis">{{ scope.row.description }}</span>
              </el-tooltip>
              <span v-else class="text-ellipsis">{{ scope.row.description || '暂无详情' }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="状态" align="center" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status == 0" size="mini" type="info">未查看</el-tag>
            <el-tag v-else-if="scope.row.status == 1" size="mini" type="warning">已查看</el-tag>
            <el-tag v-else-if="scope.row.status == 2" size="mini" type="success">已诊断</el-tag>
            <el-tag v-else-if="scope.row.status == 3" size="mini" type="danger">诊断已结束</el-tag>
            </template>
        </el-table-column>
        
        <el-table-column label="操作" align="center" width="100">
          <template slot-scope="scope">
            <el-button type="text" @click="goRecordDetail(scope.row)" class="operation-button">
              <i class="el-icon-view"></i> 查看详情
            </el-button>
            </template>
        </el-table-column>
      </el-table>

      <div v-if="recordList.length === 0" class="empty-data">
        <i class="el-icon-document"></i>
        <p>暂无病历数据</p>
      </div>
    </div>
    
    <div class="pagination">
      <el-pagination
        @current-change="(val) => handleQuery({ page: val })"
        @size-change="(val) => handleQuery({ pageSize: val })"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 30, 50]"
        :current-page="tableParams.page"
        :page-size="tableParams.pageSize"
        :total="total"
      >
      </el-pagination>
    </div>
  </div>
</template>

<script>
import { getRecordList } from '@/api/record';
import { getDoctorInfo } from '@/api/doctor';
import { isRequestCanceled, debounce, cancelAllRequests } from '@/utils/request';

export default {
  name: 'all',
  components: {

  },
  props: ['activeName'],
  data () {
    return {
      recordList: [],
      tableParams: {
        page: 1,
        pageSize: 10,
      },
      total: 0,
      form: {
        keyword: '',
        pageNum: 1,
        pageSize: 10,
      },
      loading: false,
      windowWidth: window.innerWidth,
    }
  },
  computed: {
    userInfo() {
      return this.$store.state.user.userInfo;
    },
  },
  mounted() {
    // 如果用户信息已加载，则立即查询数据
    if (this.userInfo && this.userInfo.userId) {
      this.handleQuery();
    }
    
    // 监听用户信息加载事件
    this.$bus.$on('user-info-loaded', this.onUserInfoLoaded);
    
    // 监听诊断完成事件
    this.$bus.$on('refresh-diagnosis-list', this.refreshData);
    
    // 添加窗口大小变化监听
    window.addEventListener('resize', this.handleResize);
    this.handleResize();
  },
  beforeDestroy() {
    // 移除事件监听
    this.$bus.$off('user-info-loaded', this.onUserInfoLoaded);
    this.$bus.$off('refresh-diagnosis-list', this.refreshData);
    window.removeEventListener('resize', this.handleResize);
    
    // 取消所有请求
    cancelAllRequests();
  },
  watch: {
    // 监听用户信息变化，当用户信息加载完成后查询数据
    userInfo: {
      handler(newVal) {
        if (newVal && newVal.userId) {
          this.handleQuery();
        }
      },
      immediate: true
    },
    activeName: {
      handler(newVal, oldVal) {
        // 根据选项卡设置不同的状态过滤条件
      if (newVal === 'noDiagnosis') {
          // 只显示未查看(0)和已查看(1)的病例
        this.tableParams.status = '0,1'
      } else {
          // 全部病例没有状态限制
        this.tableParams.status = ''
      }
        
        if (this.userInfo && this.userInfo.userId) {
      this.handleQuery();
        }
      },
      immediate: true
    }
  },
  methods: {
    // 处理窗口大小变化
    handleResize() {
      this.windowWidth = window.innerWidth;
    },
    
    // 设置表格行的样式
    tableRowClassName({row, rowIndex}) {
      if (row.status === 0) {
        return 'unread-row';
      } else if (row.status === 1) {
        return 'viewed-row';
      }
      return '';
    },
    
    async handleQuery(params = {}) {
      this.loading = true;
      try {
        // 确保用户是医生角色
        if (!this.userInfo || this.userInfo.role !== 1) {
          this.recordList = [];
          this.total = 0;
          return;
        }

        // 获取医生信息
        const doctor = await getDoctorInfo(this.userInfo.userId);

        // 确保医生ID存在
        if (!doctor || !doctor.doctorId) {
          this.recordList = [];
          this.total = 0;
          return;
        }

        // 更新查询参数
        this.tableParams = { 
          ...this.tableParams, 
          ...params, 
          doctorId: doctor.doctorId 
        };

        // 获取记录列表
        const result = await getRecordList(this.tableParams);
        this.recordList = result.recordList || [];
        this.total = result.total || 0;
      
      // 检查是否有未诊断的病例，并通知父组件
      if (this.activeName === 'all') {
        // 获取未诊断病例的数量
          const undiagnosedParams = {
          doctorId: doctor.doctorId,
          status: '0,1',
          page: 1,
          pageSize: 1
        };
          const undiagnosedResult = await getRecordList(undiagnosedParams);
        // 发出事件通知父组件未诊断病例的状态
          this.$emit('update-undiagnosed', (undiagnosedResult.total || 0) > 0);
        }
      } catch (error) {
        // 如果是请求取消错误，静默处理
        if (isRequestCanceled(error)) {
          return;
        }
        console.error('获取诊断列表失败:', error);
        this.$message.error('获取诊断列表失败，请稍后再试');
        // 出错时清空列表
        this.recordList = [];
        this.total = 0;
      } finally {
        this.loading = false;
      }
    }, 
    goRecordDetail(item) {
      // 先取消所有正在进行的请求，避免路由切换时出现请求取消错误
      cancelAllRequests();
      
      // 使用nextTick确保DOM更新和请求取消后再跳转
      this.$nextTick(() => {
      this.$router.push({
          path: '/diagnosis/detail',
          query: { recordId: item.recordId }
        });
      });
    },
    // 用户信息加载后的回调
    onUserInfoLoaded(userInfo) {
      if (userInfo && userInfo.userId) {
        this.handleQuery();
    }
  },
    // 刷新数据
    refreshData() {
      if (this.userInfo && this.userInfo.userId) {
        this.handleQuery();
      }
    },
    resetQuery() {
      this.form.keyword = '';
      this.tableParams.page = 1;
      this.handleQuery();
    },
  },
}
</script>

<style scoped lang='scss'>
.filter-container {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  flex-wrap: wrap;
  gap: 8px;
  background-color: rgba(255, 255, 255, 0.7);
  padding: 10px;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
  
  .filter-item {
    max-width: 300px;
    width: 100%;
    flex-shrink: 1;
    
    :deep(.el-input__inner) {
      border-color: var(--bamboo-green);
      height: 32px;
      line-height: 32px;
      
      &:focus {
        border-color: var(--cinnabar-red);
      }
    }
    
    :deep(.el-input__prefix) {
      color: var(--bamboo-green);
    }
  }
  
  .filter-button {
    flex-shrink: 0;
    padding: 7px 15px;
    
    &.el-button--primary {
      background: var(--gradient-herbal);
      border-color: var(--bamboo-green);
    }
  }
}

.table-container {
  flex: 1;
  overflow: auto;
  position: relative;
  
  .medical-table {
    width: 100%;
    border-radius: var(--border-radius);
    overflow: hidden;
    border: 1px solid var(--border-color);
    
    :deep(.el-table__row) {
      transition: all 0.3s;
      
      &:hover {
        background-color: rgba(248, 240, 221, 0.5);
      }
      
      &.unread-row {
        background-color: rgba(255, 250, 240, 0.6);
      }
      
      &.viewed-row {
        background-color: rgba(248, 240, 221, 0.3);
      }
    }
    
    :deep(.el-table__header) {
      th {
        background-color: var(--ginseng-beige);
        color: var(--rhubarb-brown);
        font-weight: 600;
        padding: 8px 0;
      }
    }
    
    :deep(.el-table__body) {
      td {
        padding: 5px 0;
      }
    }
}

  .empty-data {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 0;
    color: var(--text-secondary);
    
    i {
      font-size: 48px;
      margin-bottom: 10px;
      color: var(--licorice-tan);
    }
    
    p {
      font-size: 16px;
    }
  }
}

.patient-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  
  .patient-name {
    font-weight: 600;
    font-size: 14px;
    margin-bottom: 3px;
    color: var(--rhubarb-brown);
    
    i {
      margin-right: 5px;
      color: var(--bamboo-green);
    }
}

  .patient-details {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    font-size: 12px;
    color: var(--text-secondary);
    
    span {
      display: flex;
      align-items: center;
      
      i {
        margin-right: 4px;
        color: var(--licorice-tan);
      }
    }
  }
}

.symptom-info {
  display: flex;
  align-items: flex-start;
  
  i {
    margin-right: 8px;
    margin-top: 3px;
    color: var(--realgar-orange);
    flex-shrink: 0;
}

.text-ellipsis {
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
    white-space: nowrap;
    max-width: calc(100% - 20px);
  }
}

.operation-button {
  color: var(--indigo-blue);
  
  &:hover {
    color: var(--cinnabar-red);
  }
  
  i {
    margin-right: 3px;
  }
}

.el-tag {
  border-radius: 12px;
  padding: 0 10px;
  
  &.el-tag--info {
    background-color: var(--ginseng-beige);
    border-color: var(--licorice-tan);
    color: var(--rhubarb-brown);
  }
  
  &.el-tag--success {
    background: var(--gradient-herbal);
    border: none;
    color: white;
  }
  
  &.el-tag--warning {
    background: var(--gradient-chrysanthemum);
    border: none;
    color: white;
  }
  
  &.el-tag--danger {
    background: var(--gradient-cinnabar);
    border: none;
    color: white;
  }
}

.pagination {
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
  
  :deep(.el-pagination) {
    padding: 0;
    font-weight: normal;
    white-space: normal;
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
    justify-content: flex-end;
    
    .btn-prev, .btn-next, .el-pager li {
      background-color: #fff;
      border: 1px solid var(--border-color);
      color: var(--text-secondary);
      transition: all 0.3s;
      margin: 0 1px;
      min-width: 28px;
      height: 28px;
      line-height: 26px;
      
      &.is-active {
        background: var(--gradient-cinnabar);
        border-color: var(--cinnabar-red);
        color: #fff;
      }
      
      &:not(.disabled):hover {
        color: var(--cinnabar-red);
        border-color: var(--cinnabar-red);
      }
    }
    
    .el-pagination__sizes {
      margin-right: 8px;
      
      .el-select .el-input .el-input__inner {
        border-color: var(--border-color);
        height: 28px;
        line-height: 28px;
      }
    }
    
    .el-pagination__jump {
      margin-left: 8px;
      
      .el-pagination__editor.el-input .el-input__inner {
        height: 28px;
        line-height: 28px;
      }
    }
  }
}

/* 响应式调整 */
@media screen and (max-width: 992px) {
  .filter-container {
    .filter-item {
      max-width: none;
      flex: 1;
    }
  }
  
  .pagination {
    :deep(.el-pagination) {
      justify-content: center;
      
      .el-pagination__sizes {
        display: none !important;
      }
    }
  }
}

@media screen and (max-width: 768px) {
  .filter-container {
    flex-direction: column;
    align-items: stretch;
    
    .filter-item, .filter-button {
      width: 100%;
      max-width: none;
    }
  }
  
  .patient-info {
    .patient-details {
      flex-direction: column;
      gap: 3px;
    }
  }
  
  .pagination {
    :deep(.el-pagination) {
      justify-content: center;
      
      .el-pagination__jump {
        display: none !important;
      }
      
      .el-pager li {
        min-width: 28px;
      }
    }
  }
}
</style>