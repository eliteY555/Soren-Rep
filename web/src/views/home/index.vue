<template>
  <div style="width: 100%; height: 100%;">
    <el-container style="height: 100%; flex-direction: column">
      <el-header class="app-header">
        <div class="header-content">
          <div class="left-section">
            <div class="logo">
              <i class="el-icon-s-help"></i>
              <h2>诊易通-在线问诊平台</h2>
            </div>
            
            <el-menu 
              :default-active="defaultActive" 
              mode="horizontal"
              class="horizontal-menu" 
              router
              background-color="transparent"
              text-color="#ffffff"
              active-text-color="#ffffff">
              <el-menu-item
                v-for="item in allowedRoutes"
                :key="item.path"
                :index="item.path"
                class="horizontal-menu-item"
              >
                <i :class="item.icon"></i>
                <span>{{ item.name }}</span>
              </el-menu-item>
            </el-menu>
          </div>
          
          <el-dropdown trigger="hover" class="user-dropdown">
          <span class="el-dropdown-link">
              <i class="el-icon-user"></i>
              <span class="username">{{ userInfo && userInfo.username || '未登录' }}</span>
          </span>
          <el-dropdown-menu slot="dropdown">
              <el-dropdown-item icon="el-icon-user-solid">{{ userInfo && userInfo.username || '未登录' }}</el-dropdown-item>
            <hr />
            <el-dropdown-item icon="el-icon-edit" @click.native="userInfoVisible = true">基础信息</el-dropdown-item>
            <el-dropdown-item icon="el-icon-message" @click.native="updateInfoVisible = true">详细信息</el-dropdown-item>
            <el-dropdown-item
              icon="el-icon-switch-button"
              @click.native="logout"
              >退出系统</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="app-main">
        <div class="decorative-elements">
          <div class="cloud-pattern top-left"></div>
          <div class="cloud-pattern bottom-right"></div>
          <div class="floral-pattern top-right"></div>
          <div class="floral-pattern bottom-left"></div>
        </div>
        <keep-alive>
          <router-view></router-view>
        </keep-alive>
        </el-main>
    </el-container>

    <!-- 修改基础信息 -->
    <el-dialog title="修改基础信息" width=65% :visible.sync="userInfoVisible">
      <el-form v-if="userInfo" :model="userInfo" :rules="userInfoRule" ref="userInfoRef" label-width="80px">
        <el-row :gutter="30">
          <el-col :span="12">
            <el-form-item label="用户名">
              <el-input v-model="userInfo.username" placeholder="请输入用户名" clearable ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="userInfo.phone" placeholder="请输入手机号" clearable></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="role">
              <el-radio-group v-model="userInfo.role" disabled>
                <el-radio :label="0">患者</el-radio>
                <el-radio :label="1">医师</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="userInfo.email" placeholder="请输入邮箱" clearable></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        
        <div class="password-section">
          <div class="password-section-header">
            <h4>修改密码（可选）</h4>
            <span class="password-tip">如需修改密码，请同时填写原密码和新密码</span>
          </div>
          <el-row :gutter="30">
          <el-col :span="12">
              <el-form-item label="原密码">
              <el-input v-model="userInfo.oldPassword" type="password" placeholder="请输入原密码" autocomplete="off"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
              <el-form-item label="新密码">
              <el-input v-model="userInfo.newPassword" type="password" placeholder="请输入新密码" autocomplete="off"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        </div>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="reset('userInfoRef')">取 消</el-button>
        <el-button type="primary" @click="updateBasicInfo">确 定</el-button>
      </div>
    </el-dialog>
    <!-- 修改患者个人信息 -->
    <el-dialog v-if="userInfo && userInfo.role === 0" title="修改患者信息" width=65% :visible.sync="updateInfoVisible">
      <el-form :model="infoForm" :rules="infoRule" ref="infoFormRef" label-width="80px">
        <el-row :gutter="30">
          <el-col :span="12">
            <el-form-item label="姓名" prop="patientName">
              <el-input v-model="infoForm.patientName" placeholder="请输入姓名" clearable></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄" prop="age">
              <el-input v-model="infoForm.age" placeholder="请输入年龄" clearable></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="sex">
              <el-radio-group v-model="infoForm.sex">
                <el-radio :label="0">男</el-radio>
                <el-radio :label="1">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="既往史">
              <el-input type="textarea" v-model="infoForm.oldHistory" placeholder="请输入既往史（既往是否有类似症状、是否患有慢性疾病，如高血压、糖尿病等）" ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="过敏史">
              <el-input type="textarea" v-model="infoForm.allergiesHistory" placeholder="请输入过敏史（对药物、食物或环境因素的过敏情况）" ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生活习惯">
              <el-input type="textarea" v-model="infoForm.habits" placeholder="请输入过敏史（可以包括饮食、睡眠、情绪、烟酒史等）" ></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="reset('infoFormRef')">取 消</el-button>
        <el-button type="primary" @click="updatePatientInfo">确 定</el-button>
      </div>
    </el-dialog>
    <!-- 修改医生个人信息 -->
    <el-dialog v-if="userInfo && userInfo.role === 1" title="修改医生信息" width=65% :visible.sync="updateInfoVisible">
      <el-form :model="infoForm" :rules="infoRule" ref="infoFormRef" label-width="80px">
        <el-row :gutter="30">
          <el-col :span="12">
            <el-form-item label="姓名" prop="doctorName">
              <el-input v-model="infoForm.doctorName" placeholder="请输入姓名" clearable></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在地区">
              <el-select
                v-model="infoForm.cityName"
                clearable
                placeholder="请选择地区"
                style="width: 100%"
              >
                <el-option
                  v-for="item in cities"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                >
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在医院">
              <el-input v-model="infoForm.hospitalName" placeholder="请输入所在医院" clearable></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在科室">
              <el-input v-model="infoForm.departmentName" placeholder="请输入所在科室" clearable></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="个人简介">
              <el-input type="textarea" v-model="infoForm.introduction" placeholder="请输入个人简介" ></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="reset('infoFormRef')">取 消</el-button>
        <el-button type="primary" @click="updateDoctorInfo">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { update } from "@/api/user";
import { Encrypt } from "@/utils/secret";
import cities from "@/assets/data/cities.js";
import { updatePatientInfo, getPatientInfo, createPatientInfo } from "@/api/patient"
import { updateDoctorInfo, getDoctorInfo } from "@/api/doctor"
import { cancelAllRequests } from "@/utils/request";

export default {
  components: {},
  data() {
    return {
      defaultActive: this.$route.path,
      userInfoVisible: false,
      updateInfoVisible: false,
      // 侧边栏颜色配置
      sidebarBgColor: '#f3ece2',
      sidebarTextColor: '#60503c',
      sidebarActiveTextColor: '#8b6f4e',
      userInfoRule:{
        phone: [
          { required: true, message: "请输入手机号", trigger: "blur" },
          {
            pattern: /^1[3-9]\d{9}$/,
            message: "请输入有效的电话号码！",
            trigger: "blur",
          },
        ],
        email: [
          { required: true, message: "请输入邮箱", trigger: "blur" },
          {
            type: "email",
            message: "请输入正确的邮箱地址",
            trigger: "blur",
          },
        ],
      },
      infoForm: {
        patientName: '',
        age: '',
        sex: 0,
        oldHistory: '',
        allergiesHistory: '',
        habits: '',
        doctorName: '',
        cityName: '',
        hospitalName: '',
        departmentName: '',
        introduction: ''
      },
      infoRule: {
        patientName:[
          { required: true, message: '请输入姓名', trigger: 'blur' },
        ],
        doctorName:[
          { required: true, message: '请输入姓名', trigger: 'blur' },
        ],
        sex:[
          { required: true, message: '请选择性别', trigger: 'blur' },
        ],
        age:[
          {required: true, message: '请输入年龄', trigger: 'blur'}
        ],
      },
      cities: [],
    };
  },

  computed: {
    userInfo() {
      return this.$store.state.user.userInfo;
    },
    allowedRoutes() {
      if (!this.userInfo) {
        return [];
      }
      
      return this.$router.options.routes
        .find(route => route.path === '/home')
        .children.filter(child => {
          return child?.meta?.roles.includes(this.userInfo.role);
        });
    },
  },

  watch: {
    async updateInfoVisible(newVal) {
      if (newVal && this.userInfo) {
        try {
        if (this.userInfo.role === 0) {
            const patientInfo = await getPatientInfo(this.userInfo.userId);
            if (patientInfo) {
              this.infoForm = {
                ...this.infoForm,
                ...patientInfo
              };
            }
        } else {
            const doctorInfo = await getDoctorInfo(this.userInfo.userId);
            if (doctorInfo) {
              this.infoForm = {
                ...this.infoForm,
                ...doctorInfo
              };
            }
          }
        } catch (error) {
          console.error('获取用户信息失败:', error);
          this.$message.error('获取用户信息失败，请稍后再试');
          // 即使获取失败也不要阻止弹窗显示
        }
      }
    },
    userInfoVisible(newVal) {
      if (newVal && this.userInfo) {
        // 打开对话框时重置密码字段
        this.userInfo.oldPassword = ''
        this.userInfo.newPassword = ''
        // 确保Vue会检测到变化
        this.$nextTick(() => {
          if (this.$refs.userInfoRef) {
            this.$refs.userInfoRef.clearValidate()
          }
        })
      }
    }
  },
  
  async mounted() {
    this.cities = cities
    // 初始化密码字段，避免undefined值
    if (this.userInfo) {
    this.userInfo.oldPassword = ''
    this.userInfo.newPassword = ''
    }
  },
  methods: {
    reset(formName) {
      try {
        if (this.$refs[formName]) {
      this.$refs[formName].resetFields();
        }
      } catch (error) {
        console.error('重置表单失败:', error);
      } finally {
        // 无论如何都关闭弹窗
        this.userInfoVisible = false;
        this.updateInfoVisible = false;
      }
    },

    async updateBasicInfo() {
      this.$refs.userInfoRef.validate(async (valid) => {
        if (valid) {
          try {
            // 创建要发送的数据对象
            const updateData = { ...this.userInfo };
            
            // 只有当两个密码字段都填写时，才包含密码信息
            if (this.userInfo.oldPassword && this.userInfo.newPassword) {
              updateData.oldPassword = Encrypt(this.userInfo.oldPassword);
              updateData.newPassword = Encrypt(this.userInfo.newPassword);
            } else if ((this.userInfo.oldPassword && !this.userInfo.newPassword) || 
                      (!this.userInfo.oldPassword && this.userInfo.newPassword)) {
              // 如果只填写了一个密码字段，提示用户
              this.$message.warning("修改密码需同时填写原密码和新密码");
              return;
            }
            
            // 发送更新请求
            const res = await update(updateData);
            if (res) {
              this.$message.success("修改成功");
              this.userInfoVisible = false;
              
              // 更新本地存储的用户信息，移除密码字段
              const updatedUserInfo = { ...this.userInfo };
              delete updatedUserInfo.oldPassword;
              delete updatedUserInfo.newPassword;
              this.$store.dispatch('setUserInfo', updatedUserInfo);
            }
          } catch (error) {
            console.error('更新用户信息失败:', error);
            this.$message.error("修改失败，请稍后再试");
          }
        } else {
          this.$message.error("请填写必填项");
          return false;
        }
      });
    },

    updatePatientInfo() {
      this.$refs.infoFormRef.validate(async (valid) => {
        if (valid) {
          try {
            // 确保userId字段存在
            this.infoForm.userId = this.userInfo.userId;
            
            // 检查是否需要创建还是更新
            let isUpdate = true;
            try {
              // 尝试获取现有患者信息
              const existingPatient = await getPatientInfo(this.userInfo.userId);
              isUpdate = existingPatient && existingPatient.patientId;
            } catch (error) {
              isUpdate = false;
              console.log("患者信息不存在，将创建新记录");
            }
            
            let res;
            if (isUpdate) {
              // 更新现有记录
              res = await updatePatientInfo(this.infoForm);
            } else {
              // 创建新记录
              res = await createPatientInfo(this.infoForm);
            }
            
            if (res) {
              this.$message.success("修改成功")
              this.updateInfoVisible = false;
              
              try {
                // 重新获取最新的患者信息
                const updatedPatientInfo = await getPatientInfo(this.userInfo.userId);
                
                if (updatedPatientInfo) {
                  // 更新用户信息中的性别字段
                  const updatedUserInfo = {
                    ...this.userInfo,
                    sex: updatedPatientInfo.sex
                  };
                  
                  // 更新Vuex中的用户信息
                  this.$store.dispatch('setUserInfo', updatedUserInfo);
                  
                  // 触发事件通知其他组件患者信息已更新
                  this.$bus.$emit('user-info-updated', updatedPatientInfo);
                }
              } catch (error) {
                console.error('获取更新后的患者信息失败:', error);
                this.$message.error('获取更新后的患者信息失败，请刷新页面');
              }
            }
          } catch (error) {
            console.error('更新患者信息失败:', error);
            this.$message.error("保存失败，请稍后再试");
          }
        }
      })
    },

    updateDoctorInfo() {
      this.$refs.infoFormRef.validate(async (valid) => {
        if (valid) {
          try {
            const res = await updateDoctorInfo(this.infoForm)
            if (res) {
              this.$message.success("修改成功")
              this.updateInfoVisible = false;
            }
          } catch (error) {
            console.log(error);
          }
        }
      })
    },

    logout() {
      // 弹框二次确认
      this.$confirm('确定退出登录吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 清空localStorage
        this.$store.dispatch('clearUserInfo');
        this.$router.push({path: '/'})
        this.$message({
          type: 'success',
          message: '退出登录成功!'
        });
      }).catch(() => {
        // this.$message({
        //   type: 'info',
        //   message: '已取消退出登录'
        // });          
      });
    },
  },
  beforeDestroy() {
    // 取消所有未完成的请求
    cancelAllRequests();
  },
};
</script>

<style scoped lang="scss">
/* 页头样式 */
.app-header {
  padding: 0;
  background-image: var(--gradient-header);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  height: 60px; /* 固定高度确保一致性 */
  line-height: normal;
  position: relative;
  overflow: visible; /* 修改为visible，确保菜单可以溢出显示 */
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: url('@/assets/images/patterns/cloud-pattern.svg');
    opacity: 0.15;
    z-index: 0;
  }
  
  .header-content {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    position: relative;
    z-index: 1;
    
    .left-section {
      display: flex;
      align-items: center;
      height: 100%;
      min-width: 0; /* 允许子元素压缩 */
      
      .logo {
        display: flex;
        align-items: center;
        margin-right: 20px;
        flex-shrink: 0; /* 防止logo被压缩 */
        
        i {
          font-size: 24px;
          color: white;
          margin-right: 10px;
        }
        
        h2 {
          margin: 0;
          color: white;
          font-size: 18px;
          font-weight: 600;
          white-space: nowrap; /* 防止文本换行 */
        }
      }
      
      .horizontal-menu {
        border-bottom: none;
        background-color: transparent;
        height: 100%;
        flex: 1;
        min-width: 0; /* 允许菜单项压缩 */
        overflow-x: auto; /* 允许横向滚动 */
        display: flex !important; /* 强制显示菜单 */
        
        /* 隐藏滚动条但保留功能 */
        &::-webkit-scrollbar {
          height: 0;
          width: 0;
          display: none;
        }
        
        .horizontal-menu-item {
          height: 60px;
          line-height: 60px;
          color: rgba(255, 255, 255, 0.85);
          font-size: 15px;
          padding: 0 15px;
          border-bottom: 3px solid transparent;
          transition: all 0.3s;
          white-space: nowrap; /* 防止文本换行 */
          display: inline-block !important; /* 强制显示菜单项 */
          
          &:hover, &.is-active {
            background-color: rgba(255, 255, 255, 0.1);
            color: #ffffff;
            border-bottom-color: #ffffff;
          }
          
          i {
            margin-right: 5px;
            font-size: 16px;
            vertical-align: middle;
          }
        }
      }
    }
    
    .user-dropdown {
      cursor: pointer;
      margin-left: 15px;
      flex-shrink: 0; /* 防止用户下拉菜单被压缩 */
      
      .el-dropdown-link {
        display: flex;
        align-items: center;
        color: white;
        
        i {
          font-size: 18px;
          margin-right: 8px;
        }
        
        .username {
          font-size: 14px;
          max-width: 100px; /* 限制用户名长度 */
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }
  }
}

/* 侧边栏样式 */
.app-sidebar {
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
  border-right: 1px solid var(--border-color);
  background-color: var(--bg-sidebar);
  
  .sidebar-menu {
    border-right: none;
    
    .sidebar-header {
      height: 50px;
      display: flex;
      align-items: center;
      padding-left: 20px;
      font-weight: bold;
      color: var(--primary-color-dark);
      border-bottom: 1px dashed var(--border-color);
      
      i {
        margin-right: 8px;
      }
    }
    
    .sidebar-item {
      margin: 8px 0;
      
      &:hover {
        background-color: rgba(139, 111, 78, 0.1);
      }
      
      &.is-active {
        position: relative;
        
        &::after {
          content: "";
          position: absolute;
          top: 0;
          left: 0;
  height: 100%;
          width: 4px;
          background-color: var(--primary-color);
        }
      }
    }
  }
}

/* 主内容区域样式 */
.app-main {
  background-color: var(--bg-light);
  padding: 20px;
  overflow-y: auto;
  height: calc(100vh - 60px);
  position: relative;
  
  /* 装饰元素 */
  .decorative-elements {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
    z-index: 0;
    overflow: hidden;
    
    .cloud-pattern {
      position: absolute;
      width: 300px;
      height: 300px;
      background-image: url('@/assets/images/patterns/cloud-pattern.svg');
      background-size: 300px;
      background-repeat: no-repeat;
      opacity: 0.1;
      
      &.top-left {
        top: -100px;
        left: -100px;
        transform: rotate(-15deg);
      }
      
      &.bottom-right {
        bottom: -100px;
        right: -100px;
        transform: rotate(15deg);
      }
    }
    
    .floral-pattern {
      position: absolute;
      width: 250px;
      height: 250px;
      background-image: url('@/assets/images/patterns/floral-pattern.svg');
      background-size: 250px;
      background-repeat: no-repeat;
      opacity: 0.1;
      
      &.top-right {
        top: -50px;
        right: -50px;
        transform: rotate(15deg);
      }
      
      &.bottom-left {
        bottom: -50px;
        left: -50px;
        transform: rotate(-15deg);
      }
    }
  }
  
  /* 内容区域 */
  router-view {
    position: relative;
    z-index: 1;
  }
}

/* 对话框样式增强 */
:deep(.el-dialog) {
  border-radius: 8px;
  overflow: hidden;
  
  .el-dialog__header {
    background-color: var(--bg-sidebar);
    padding: 15px 20px;
    
    .el-dialog__title {
      font-weight: 600;
      color: var(--primary-color);
    }
  }
  
  .el-dialog__body {
    padding: 20px 25px;
  }
  
  .el-dialog__footer {
    border-top: 1px solid var(--border-color);
    padding: 12px 20px;
  }
}

/* 表单样式增强 */
:deep(.el-form) {
  .el-form-item__label {
    font-weight: 500;
  }
  
  .el-input__inner:focus {
    border-color: var(--primary-color);
  }
}

/* 页面响应式 */
@media screen and (max-width: 768px) {
  .app-header {
    height: auto;
    min-height: 60px; /* 确保最小高度 */
    padding: 5px 0;
    
    .header-content {
      flex-wrap: wrap; /* 允许在小屏幕上换行 */
      
      .left-section {
        flex-wrap: nowrap; /* 防止logo和菜单换行 */
        overflow-x: auto; /* 允许横向滚动 */
        width: 100%;
        
        /* 隐藏滚动条但保留功能 */
        &::-webkit-scrollbar {
          height: 0;
          width: 0;
          display: none;
        }
        
        .logo {
          margin-right: 15px;
          padding: 5px 0;
          
          i {
            font-size: 20px;
          }
          
          h2 {
            font-size: 16px;
          }
        }
        
        .horizontal-menu {
          .horizontal-menu-item {
            height: 50px;
            line-height: 50px;
            padding: 0 10px;
            font-size: 14px;
          }
        }
      }
      
      .user-dropdown {
        margin-left: auto; /* 推到右侧 */
        padding: 5px 0;
      }
    }
  }
  
  .app-main {
    height: calc(100vh - 120px);
  }
}

/* 添加密码部分的样式 */
.password-section {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e0e0e0;
  
  .password-section-header {
    display: flex;
    align-items: center;
    margin-bottom: 15px;
    
    h4 {
      margin: 0;
      font-size: 16px;
      color: #606266;
    }
    
    .password-tip {
      margin-left: 15px;
      font-size: 13px;
      color: #909399;
    }
  }
}
</style>
