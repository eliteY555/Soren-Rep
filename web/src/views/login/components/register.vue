<template>
  <div class="register-container">
    <el-form
      ref="ruleFormRef"
      :model="ruleForm"
      :rules="rules"
      label-width="80px"
      class="register-form"
    >
      <el-form-item label="角色" prop="role">
        <el-radio-group v-model="ruleForm.role" @change="handleRoleChange">
          <el-radio :label="0">患者</el-radio>
          <el-radio :label="1">医师</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="姓名" prop="username">
        <el-input
          placeholder="请输入姓名"
          v-model="ruleForm.username"
          clearable
          autocomplete="off"
        >
        </el-input>
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input
          placeholder="请输入手机号"
          v-model="ruleForm.phone"
        ></el-input>
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input placeholder="请输入邮箱" v-model="ruleForm.email"></el-input>
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          type="password"
          placeholder="请输入密码"
          v-model="ruleForm.password"
          autocomplete="off"
        >
        </el-input>
      </el-form-item>
      <el-form-item label="确认密码" prop="checkPass">
        <el-input
          type="password"
          placeholder="请确认密码"
          v-model="ruleForm.checkPass"
          autocomplete="off"
        ></el-input>
      </el-form-item>
      
      <!-- 医生特有信息 -->
      <template v-if="ruleForm.role === 1">
        <el-divider content-position="center">医师信息</el-divider>
        
        <el-form-item label="所在城市" prop="cityName">
          <el-input placeholder="请输入所在城市" v-model="ruleForm.cityName"></el-input>
        </el-form-item>
        
        <el-form-item label="所在科室" prop="departmentName">
          <el-select v-model="ruleForm.departmentName" placeholder="请选择科室">
            <el-option v-for="item in departmentOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="所属医院" prop="hospitalName">
          <el-input placeholder="请输入所属医院" v-model="ruleForm.hospitalName"></el-input>
        </el-form-item>
        
        <el-form-item label="专业背景" prop="introduction">
          <el-input type="textarea" :rows="3" placeholder="请简要介绍您的专业背景、擅长领域等" v-model="ruleForm.introduction"></el-input>
        </el-form-item>
      </template>
      
      <div class="extra-warp">
        <el-button class="submit-btn" @click="cancel">取消</el-button>
        <el-button type="primary" class="submit-btn" @click="register" :loading="loading"
          >注册</el-button
        >
      </div>
    </el-form>
  </div>
</template>

<script>
import { patientRegister } from "@/api/patient";
import { doctorRegister } from "@/api/doctor";
export default {
  components: {},
  data() {
    var validatePass = (rule, value, callback) => {
      if (value === "") {
        callback(new Error("请再次输入密码"));
      } else if (value !== this.ruleForm.password) {
        callback(new Error("两次输入密码不一致!"));
      } else {
        callback();
      }
    };
    return {
      ruleFormRef: null,
      ruleForm: {
        role: 0,
        username: "",
        password: "",
        checkPass: "",
        phone: "",
        email: "",
        // 医生特有信息
        cityName: "",
        hospitalName: "",
        departmentName: "",
        introduction: ""
      },
      rules: {
        role: [
          { required: true, message: "请选择用户角色", trigger: "change" },
        ],
        username: [
          { required: true, message: "请输入姓名", trigger: "blur" },
        ],
        password: [
            { required: true, message: "请输入密码", trigger: "blur" },
            { min: 6, message: '密码不少于6个字符', trigger: 'blur' }
        ],
        checkPass: [
          { validator: validatePass, required: true, trigger: "blur" },
        ],
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
        // 医生特有信息验证规则
        cityName: [
          { required: true, message: "请输入所在城市", trigger: "blur" }
        ],
        hospitalName: [
          { required: true, message: "请输入所属医院", trigger: "blur" }
        ],
        departmentName: [
          { required: true, message: "请选择所在科室", trigger: "change" }
        ],
        introduction: [
          { required: true, message: "请输入专业背景", trigger: "blur" }
        ]
      },
      // 科室选项
      departmentOptions: [
        { value: '心血管内科', label: '心血管内科' },
        { value: '神经外科', label: '神经外科' },
        { value: '呼吸科', label: '呼吸科' },
        { value: '骨科', label: '骨科' },
        { value: '儿科', label: '儿科' },
        { value: '消化内科', label: '消化内科' },
        { value: '眼科', label: '眼科' },
        { value: '皮肤科', label: '皮肤科' },
        { value: '妇产科', label: '妇产科' },
        { value: '中西医结合科', label: '中西医结合科' },
        { value: '针灸科', label: '针灸科' },
        { value: '推拿科', label: '推拿科' }
      ],
      loading: false
    };
  },
  mounted() {},
  methods: {
    // 角色变更时处理
    handleRoleChange(role) {
      // 重置表单验证
      this.$nextTick(() => {
        this.$refs.ruleFormRef.clearValidate();
      });
    },
    cancel() {
      this.$refs.ruleFormRef.resetFields();
      this.$emit("switchToLogin");
    },
    register() {
      this.$refs.ruleFormRef.validate(async (valid) => {
        if (valid) {
          try {
            this.loading = true;
            // 密码明文传输，服务端 BCrypt 哈希存储
            const password = this.ruleForm.password;

            if (this.ruleForm.role === 0) {
              // 患者注册：直接写入 patient 表
              const patientData = {
                patientName: this.ruleForm.username,
                password,
                phone: this.ruleForm.phone,
                email: this.ruleForm.email,
                sex: 0,
                age: null
              };
              console.log("开始注册患者...");
              const res = await patientRegister(patientData);
              console.log("患者注册响应:", res);
              if (res && res.patientId) {
                console.log(`患者注册成功，patientId: ${res.patientId}`);
                this.$message.success("患者注册成功");
                this.$emit("switchToLogin");
              } else {
                this.$message.error("注册异常，请稍后再试");
              }
            } else {
              // 医生注册：直接写入 doctor 表
              const doctorData = {
                doctorName: this.ruleForm.username,
                password,
                phone: this.ruleForm.phone,
                email: this.ruleForm.email,
                cityName: this.ruleForm.cityName,
                hospitalName: this.ruleForm.hospitalName,
                departmentName: this.ruleForm.departmentName,
                introduction: this.ruleForm.introduction
              };
              console.log("开始注册医生...");
              const res = await doctorRegister(doctorData);
              console.log("医生注册响应:", res);
              if (res && res.doctorId) {
                console.log(`医生注册成功，doctorId: ${res.doctorId}`);
                this.$message.success("医师注册成功");
                this.$emit("switchToLogin");
              } else {
                this.$message.error("注册异常，请稍后再试");
              }
            }
          } catch (error) {
            console.error("注册失败:", error);
            let errorMsg = "注册失败";
            if (error.response && error.response.data && error.response.data.msg) {
              errorMsg = error.response.data.msg;
            } else if (error.message) {
              errorMsg += `: ${error.message}`;
            }
            this.$message.error(errorMsg);
          } finally {
            this.loading = false;
          }
        }
      });
    }
  }
};
</script>
<style scoped lang="scss">
.register-container {
  max-height: 420px;
  overflow-y: auto;
  padding-right: 10px;
  
  &::-webkit-scrollbar {
    width: 5px;
  }
  
  &::-webkit-scrollbar-track {
    background: rgba(0, 0, 0, 0.05);
    border-radius: 3px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: var(--indigo-blue, #5d7599);
    opacity: 0.7;
    border-radius: 3px;
  }
}

.register-form {
  padding: 0;
}

.el-form-item {
  margin-bottom: 12px;
}

.submit-btn {
  width: 120px;
  letter-spacing: 2px;
  padding: 9px 0;
  margin-top: 10px;
}

.extra-warp {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-top: 10px;
}

.el-divider {
  margin: 12px 0;
}
</style>
