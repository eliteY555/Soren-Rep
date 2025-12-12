<template>
  <div class="app-container">
    <div class="card-container">
      <div class="card-header">医师列表</div>
      <div class="card-body">
        <div class="search-area">
          <el-form :inline="true">
            <el-form-item>
              <el-select v-model="regionValue" placeholder="请选择地区" clearable>
                <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="hospitalValue" placeholder="请选择医院" clearable>
                <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="departmentValue" placeholder="请选择科室" clearable>
                <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onSubmit">查询</el-button>
              <el-button @click="onReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="doctor-list">
          <el-row :gutter="20">
            <el-col v-for="(doctor, index) in displayedDoctors" :key="index" :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
              <div class="doctor-card">
                <div class="doctor-avatar">
                  <img :src="doctor.avatar" alt="doctor avatar" />
                </div>
                <div class="doctor-info">
                  <div class="doctor-name">
                    {{ doctor.name }}
                    <span class="doctor-tag">{{ doctor.department }}</span>
                  </div>
                  <div class="doctor-hospital">
                    <i class="el-icon-location"></i>
                    {{ doctor.hospital }}
                  </div>
                  <div class="doctor-description">
                    简介：{{ doctor.description }}
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
        <div class="pagination-container">
          <el-pagination background 
            @size-change="handleSizeChange" 
            @current-change="handleCurrentChange"
            :current-page.sync="currentPage" 
            :page-size="10" 
            :page-sizes="[10, 20]" 
            layout="total, prev, pager, next" 
            :total="total">
          </el-pagination>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getDoctorList, getDoctorListByPage } from "@/api/doctor";
import { debounce, isRequestCanceled } from '@/utils/request';

export default {
  name: "DocList",
  data() {
    return {
      loading: false,
      regionValue: "",
      hospitalValue: "",
      departmentValue: "",
      currentPage: 1,
      pageSize: 10,
      total: 0,
      options: [
        { value: "option1", label: "选项1" },
        { value: "option2", label: "选项2" },
        { value: "option3", label: "选项3" },
      ],
      doctorsData: []
    };
  },
  computed: {
    displayedDoctors() {
      return this.doctorsData;
    }
  },
  methods: {
    handleSizeChange(val) {
      this.pageSize = val;
      this.fetchData();
    },
    handleCurrentChange(val) {
      this.currentPage = val;
      this.fetchData();
    },
    onSubmit() {
      this.currentPage = 1;
      this.fetchData();
    },
    onReset() {
      this.regionValue = "";
      this.hospitalValue = "";
      this.departmentValue = "";
      this.currentPage = 1;
      this.fetchData();
    },
    async fetchData() {
      this.loading = true;
      try {
        console.log('开始加载医生数据...');
        
        // 准备查询参数
        const params = {
          page: this.currentPage,
          pageSize: this.pageSize
        };
        
        // 添加可选筛选条件
        if (this.regionValue) {
          params.cityName = this.regionValue;
        }
        if (this.hospitalValue) {
          params.hospitalName = this.hospitalValue;
        }
        if (this.departmentValue) {
          params.departmentName = this.departmentValue;
        }
        
        console.log('查询参数:', params);
        
        // 使用分页API获取医生列表
        const result = await getDoctorListByPage(params);
        
        if (result && result.doctorList) {
          // 将API返回的数据转换为组件所需的格式
          this.doctorsData = result.doctorList.map(doctor => ({
            name: doctor.doctorName,
            department: doctor.departmentName || '未设置科室',
            hospital: doctor.hospitalName || '未设置医院',
            description: doctor.introduction || '暂无简介',
            avatar: "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png",
            doctorId: doctor.doctorId
          }));
          
          this.total = result.total || 0;
          console.log(`成功加载 ${this.doctorsData.length} 名医生数据，总数: ${this.total}`);
        } else {
          console.warn('未能获取医生数据或数据格式不正确:', result);
          this.doctorsData = [];
          this.total = 0;
        }
      } catch (error) {
        // 忽略取消的请求
        if (isRequestCanceled(error)) {
          console.log('医生列表加载请求已取消');
          return;
        }
        
        console.error('获取医生列表失败:', error);
        this.$message.error('获取医生列表失败，请稍后再试');
        this.doctorsData = [];
        this.total = 0;
      } finally {
        this.loading = false;
      }
    }
  },
  mounted() {
    this.fetchData();
  },
  beforeDestroy() {
    // 可能的清理工作
  }
};
</script>
<style scoped lang="scss">
.app-container {
  height: calc(100vh - 110px);
  padding: 15px;
  
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
      padding: 10px 15px;
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
      overflow-y: auto;
      padding: 0 15px 5px;
      
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
  }
}

.search-area {
  background: #fff;
  border-radius: 8px;
  padding: 15px 15px 5px;
  margin-bottom: 15px;
  border: 1px solid #ebeef5;
}

.doctor-card {
  border-radius: 8px;
  border: 1px solid #ebeef5;
  transition: all 0.3s;
  height: 120px;
  display: flex;
  align-items: center;
  background: linear-gradient(to right, #ffffff, #f8fbff);
  margin-bottom: 3px;
  
  &:hover {
    box-shadow: 0 5px 15px rgba(0, 110, 190, 0.1);
    transform: translateY(-3px);
    border-left: 4px solid var(--indigo-blue);
  }
}

.doctor-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 15px;
  border: 3px solid #ebeef5;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.doctor-info {
  flex: 1;
  padding-right: 15px;
}

.doctor-name {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 5px;
  display: flex;
  align-items: center;
  
  .doctor-tag {
    font-size: 12px;
    padding: 2px 8px;
    background: #e8f4ff;
    color: var(--indigo-blue);
    border-radius: 12px;
    margin-left: 10px;
    font-weight: normal;
  }
}

.doctor-hospital {
  font-size: 14px;
  color: #606266;
  margin-bottom: 5px;
  
  i {
    margin-right: 5px;
    color: var(--indigo-blue);
  }
}

.doctor-description {
  font-size: 13px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.3;
}

.doctor-list {
  margin-bottom: 10px;
  
  .el-row {
    margin-bottom: 0 !important;
  }
  
  .el-col {
    margin-bottom: 8px !important;
  }
}

.pagination-container {
  background-color: #fff;
  padding: 8px;
  border-radius: 8px;
  text-align: right;
  border: 1px solid #ebeef5;
}
</style>
