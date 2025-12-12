<template>
  <div class="add-record-container">
    <div class="card-container">
      <div class="card-header">提交病历</div>
      <div class="card-content">
        <el-form :model="recordInfoForm" :rules="addRecordRules" ref="recordInfoFormRef" label-width="80px" class="record-form" size="small">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="姓名">
              <el-input v-model="patientInfoForm.patientName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
            <el-col :xs="24" :sm="6" :md="4">
            <el-form-item label="年龄">
                <el-input type="number" v-model="patientInfoForm.age" placeholder="年龄" />
            </el-form-item>
          </el-col>
            <el-col :xs="24" :sm="6" :md="4">
            <el-form-item label="性别" class="gender-form-item">
                <el-radio-group v-model="patientInfoForm.sex" disabled class="horizontal-radio">
                  <el-radio :label="0">男</el-radio>
                  <el-radio :label="1">女</el-radio>
                </el-radio-group>
            </el-form-item>
          </el-col>
            <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="手机号">
                <el-input v-if="userInfo" v-model="userInfo.phone" placeholder="请输入手机号" clearable />
                <el-input v-else placeholder="请输入手机号" clearable disabled />
            </el-form-item>
          </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
            <el-form-item label="既往史">
                <el-input type="textarea" v-model="patientInfoForm.oldHistory" placeholder="请输入既往史" :rows="2" />
            </el-form-item>
          </el-col>
            <el-col :xs="24" :sm="12">
            <el-form-item label="过敏史">
                <el-input type="textarea" v-model="patientInfoForm.allergiesHistory" placeholder="请输入过敏史" :rows="2" />
            </el-form-item>
            </el-col>
          </el-row>

            <el-form-item label="生活习惯">
            <el-input type="textarea" v-model="patientInfoForm.habits" placeholder="请输入生活习惯（饮食、睡眠、情绪等）" :rows="2" />
            </el-form-item>

            <el-form-item prop="doctorId" label="选择医生">
              <div class="filter-info" v-if="docList.length > 0">
                <span>已找到 {{ docList.length }} 名符合条件的医生</span>
              </div>
              <div class="doctor-select-row">
                <el-select v-model="cityName" clearable placeholder="选择地区" @change="updateHospitalList">
                  <el-option v-for="item in cities" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
                <el-select v-model="hospitalName" clearable placeholder="选择医院" @change="updateDepartmentList">
                  <el-option v-for="item in hospitalList" :key="item.label" :label="item.label" :value="item.value" />
              </el-select>
                <el-select v-model="departmentName" clearable placeholder="选择科室" @change="updateDoctorList">
                  <el-option v-for="item in departmentList" :key="item.label" :label="item.label" :value="item.value" />
              </el-select>
              <el-select
                v-model="recordInfoForm.doctorId"
                clearable
                  placeholder="选择医生"
                  :loading="!allDoctors.length"
                  :disabled="!departmentName">
                  <div class="select-tip" slot="empty">
                    {{ departmentName ? (docList.length ? '暂无医生' : '正在加载...') : '请先选择科室' }}
                  </div>
                <el-option
                  v-for="item in docList"
                  :key="item.doctorId"
                  :label="item.doctorName"
                    :value="item.doctorId" />
              </el-select>
              </div>
              <div class="filter-tips" v-if="cityName || hospitalName || departmentName">
                <span>当前筛选条件: </span>
                <el-tag size="mini" v-if="cityName" closable @close="clearFilter('city')">{{ cityName }}</el-tag>
                <el-tag size="mini" v-if="hospitalName" type="success" closable @close="clearFilter('hospital')">{{ hospitalName }}</el-tag>
                <el-tag size="mini" v-if="departmentName" type="warning" closable @close="clearFilter('department')">{{ departmentName }}</el-tag>
              </div>
            </el-form-item>

            <el-form-item label="病情描述" prop="description">
            <el-input type="textarea" v-model="recordInfoForm.description" placeholder="包括现在的症状以及既往史" :rows="3" />
            </el-form-item>

          <el-form-item>
        <el-button type="primary" @click="addRecord">提交病历</el-button>
        <el-button @click="resetForm">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import { getPatientInfo } from "@/api/patient";
import { getDoctorList } from "@/api/doctor";
import { addRecord } from "@/api/record";
import { cancelAllRequests, isRequestCanceled } from "@/utils/request";

export default {
  components: {},
  data() {
    return {
      patientInfoForm: {
        patientName: '',
        age: '',
        oldHistory: '',
        allergiesHistory: '',
        habits: ''
      },
      recordInfoForm: {
        doctorId: '',
        description: '',
      },
      addRecordRules: {
        doctorId: [
          { required: true, message: "请选择一个医生", trigger: "blur" },
        ],
        description: [
          { required: true, message: "病症描述不能为空", trigger: "blur" },
        ],
      },
      cities: [],
      hospitalList: [],
      departmentList: [],
      cityName: '',
      hospitalName: '',
      departmentName: '',
      docList: [],
      allDoctors: []
    };
  },

  computed: {
    userInfo() {
      return this.$store.state.user.userInfo;
    },
  },
  async mounted() {
    // 添加事件监听
    this.$bus.$on('user-info-updated', this.handlePatientInfoUpdate);
    
    // 初始化数据
    await this.initializeData();
  },

  beforeDestroy() {
    // 组件销毁时，移除事件监听
    this.$bus.$off('user-info-updated', this.handlePatientInfoUpdate);
    
    // 取消所有未完成的请求
    cancelAllRequests();
  },

  methods: {
    // 处理患者信息更新
    async handlePatientInfoUpdate(updatedPatientInfo) {
      try {
        // 如果传入了更新后的患者信息，直接使用它
        if (updatedPatientInfo) {
          this.patientInfoForm = {
            ...this.patientInfoForm,
            ...updatedPatientInfo
          };
        } else {
          // 否则重新从服务器获取
          await this.fetchPatientInfo();
        }
      } catch (error) {
        // 忽略请求取消错误
        if (isRequestCanceled(error)) {
          console.log('患者信息更新请求被取消');
          return;
        }
        console.error('处理患者信息更新失败:', error);
      }
    },
    
    // 获取患者信息
    async fetchPatientInfo() {
      try {
        if (this.userInfo?.userId) {
          const patientInfo = await getPatientInfo(this.userInfo.userId);
          if (patientInfo) {
            this.patientInfoForm = {
              ...this.patientInfoForm,
              ...patientInfo
            };
          }
        }
      } catch (error) {
        // 忽略请求取消错误
        if (isRequestCanceled(error)) {
          console.log('获取患者信息请求被取消');
          return;
        }
        console.error('获取患者信息失败:', error);
        this.$message.error('获取患者信息失败，请稍后再试');
      }
    },
    
    // 初始化数据
    async initializeData() {
      try {
        // 获取患者信息
        await this.fetchPatientInfo();
        
        // 获取医生列表
        try {
          this.allDoctors = await getDoctorList() || [];
          
          // 处理城市列表
          if (this.allDoctors.length > 0) {
            this.cities = [...new Set(this.allDoctors.map(doctor => doctor.cityName))]
              .filter(Boolean)
              .map(city => ({
                label: city,
                value: city
              }));
            this.updateHospitalList();
          }
        } catch (doctorError) {
          // 处理医生列表加载错误
          if (isRequestCanceled(doctorError)) {
            console.log('医生列表加载被取消');
            return;
          }
          console.error('获取医生列表失败:', doctorError);
          this.$message.error('获取医生列表失败，请刷新页面重试');
        }
      } catch (error) {
        // 根据不同错误类型进行处理
        if (isRequestCanceled(error)) {
          console.log('数据初始化请求被取消，可能是由于路由切换');
          return;
        }
        console.error('初始化数据失败:', error);
        this.$message.error('加载数据失败，请稍后再试');
      }
    },

    addRecord() {
      this.$refs.recordInfoFormRef.validate(async (valid) => {
        if (valid) {
          const recordData = {
            ...this.patientInfoForm, 
            ...this.recordInfoForm,
            phone: this.userInfo.phone // 确保手机号码被包含在提交数据中
          };
          // 确保使用正确的性别值
          recordData.sex = this.patientInfoForm.sex;
          
          try {
            const result = await addRecord(recordData);
          if (result) {
            this.$message.success("病历创建成功");
              // 提交成功后返回列表页
              this.$router.push('/home/allRecords');
            }
          } catch (error) {
            console.error('提交病历失败:', error);
            this.$message.error('提交失败，请检查表单信息');
          }
        } else {
          this.$message.error('请完善表单信息');
        }
      });
    },
    resetForm() {
      this.$refs.recordInfoFormRef.resetFields();
      this.cityName = '';
      this.hospitalName = '';
      this.departmentName = '';
      this.updateHospitalList();
    },
    
    // 清除特定筛选条件
    clearFilter(type) {
      switch(type) {
        case 'city':
          this.cityName = '';
          this.updateHospitalList();
          break;
        case 'hospital':
          this.hospitalName = '';
          this.updateDepartmentList();
          break;
        case 'department':
          this.departmentName = '';
          this.updateDoctorList();
          break;
      }
    },

    updateHospitalList() {
      // 当城市选择为空时，显示所有医院
      if (!this.cityName) {
        const hospitals = this.allDoctors
          .map(doctor => doctor.hospitalName);
        this.hospitalList = [...new Set(hospitals)]
          .filter(Boolean)
          .map(hospital => ({
            label: hospital,
            value: hospital
          }));
        // 不重置医院选择，保留用户之前的选择
        this.updateDepartmentList();
        return;
      }
      
      // 根据选择的城市筛选医院
      const hospitals = this.allDoctors
        .filter(doctor => doctor.cityName === this.cityName)
        .map(doctor => doctor.hospitalName);
      this.hospitalList = [...new Set(hospitals)]
        .filter(Boolean)
        .map(hospital => ({
        label: hospital,
        value: hospital
      }));
        
      // 检查当前选择的医院是否在筛选结果中
      const hospitalExists = this.hospitalList.some(h => h.value === this.hospitalName);
      if (!hospitalExists) {
        this.hospitalName = ''; // 只有当选择的医院不在筛选结果中时才重置
      }
      
      this.updateDepartmentList();
    },
    
    updateDepartmentList() {
      // 构建筛选条件
      let filteredDoctors = this.allDoctors;
      
      // 应用城市筛选
      if (this.cityName) {
        filteredDoctors = filteredDoctors.filter(doctor => doctor.cityName === this.cityName);
      }
      
      // 当医院选择为空时，显示当前筛选条件下的所有科室
      if (!this.hospitalName) {
        const departments = filteredDoctors
          .map(doctor => doctor.departmentName);
        this.departmentList = [...new Set(departments)]
          .filter(Boolean)
          .map(department => ({
            label: department,
            value: department
          }));
        // 不重置科室选择，保留用户之前的选择
        this.updateDoctorList();
        return;
      }
      
      // 应用医院筛选
      filteredDoctors = filteredDoctors.filter(doctor => doctor.hospitalName === this.hospitalName);
      
      // 获取科室列表
      const departments = filteredDoctors
        .map(doctor => doctor.departmentName);
      this.departmentList = [...new Set(departments)]
        .filter(Boolean)
        .map(department => ({
        label: department,
        value: department
      }));
        
      // 检查当前选择的科室是否在筛选结果中
      const departmentExists = this.departmentList.some(d => d.value === this.departmentName);
      if (!departmentExists) {
        this.departmentName = ''; // 只有当选择的科室不在筛选结果中时才重置
      }
      
      this.updateDoctorList();
    },
    
    updateDoctorList() {
      // 构建筛选条件
      let filteredDoctors = this.allDoctors;
      
      // 应用所有已选择的筛选条件
      if (this.cityName) {
        filteredDoctors = filteredDoctors.filter(doctor => doctor.cityName === this.cityName);
      }
      
      if (this.hospitalName) {
        filteredDoctors = filteredDoctors.filter(doctor => doctor.hospitalName === this.hospitalName);
      }
      
      if (this.departmentName) {
        filteredDoctors = filteredDoctors.filter(doctor => doctor.departmentName === this.departmentName);
      } else {
        // 如果没有选择科室，则清空医生列表
        this.docList = [];
        this.recordInfoForm.doctorId = '';
        return;
      }
      
      // 更新医生列表
      this.docList = filteredDoctors.map(doctor => ({
          doctorName: doctor.doctorName,
          doctorId: doctor.doctorId
        }));
      
      // 检查当前选择的医生是否在筛选结果中
      const doctorExists = this.docList.some(d => d.doctorId === this.recordInfoForm.doctorId);
      if (!doctorExists) {
        this.recordInfoForm.doctorId = ''; // 只有当选择的医生不在筛选结果中时才重置
      }
    },
  },
};
</script>
<style scoped lang="scss">
.add-record-container {
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
    
    .card-content {
      flex: 1;
      overflow-y: auto;
      padding: 10px 15px;
      
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

.record-form {
  background-color: white;
  border-radius: 8px;
  padding: 15px;
  
  .el-form-item {
    margin-bottom: 12px;
  }
}

.doctor-select-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
  
  .el-select {
    width: 140px;
    
    @media (max-width: 768px) {
      width: 100%;
      margin-bottom: 5px;
    }
  }
  
  @media (max-width: 768px) {
    flex-direction: column;
  }
}

.filter-info {
  font-size: 13px;
  color: #409EFF;
  margin-bottom: 8px;
}

.filter-tips {
  margin-top: 8px;
  font-size: 12px;
  
  span {
    color: #606266;
    margin-right: 5px;
  }
  
  .el-tag {
    margin-right: 5px;
    cursor: pointer;
  }
}

.select-tip {
  padding: 8px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

@media (max-width: 768px) {
  .add-record-container {
    padding: 10px;
    
    .card-container {
      .card-content {
        padding: 8px;
      }
    }
  }
  
  .record-form {
    padding: 10px;
  }
}

.horizontal-radio {
  display: flex;
  flex-direction: row;
  
  .el-radio {
    margin-right: 20px;
    margin-left: 0;
  }
  
  .el-radio:last-child {
    margin-right: 0;
  }
}

.gender-form-item {
  ::v-deep .el-form-item__label {
    line-height: 36px; /* 与单选按钮高度一致 */
    padding-top: 0;
    margin-top: 0;
  }
  
  ::v-deep .el-form-item__content {
    display: flex;
    align-items: center;
    min-height: 36px;
  }
}
</style>
