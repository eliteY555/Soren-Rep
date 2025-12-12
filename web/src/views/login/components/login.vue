<template>
  <div class="login-form-container">
    <el-form ref="ruleFormRef" :model="ruleForm" :rules="rules">
      <el-form-item prop="identity">
        <el-input
          text
          placeholder="请输入用户名/手机号/邮箱"
          v-model="ruleForm.identity"
          clearable
          autocomplete="off"
          @keyup.enter.native="login"
        >
          <template #prefix>
            <i class="el-icon-user"></i>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          type="password"
          placeholder="请输入密码"
          v-model="ruleForm.password"
          show-password
          autocomplete="off"
          @keyup.enter.native="login"
        >
          <template #prefix>
            <i class="el-icon-lock"></i>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          class="submit-btn"
          round
          @click="login"
          :loading="loading"
          >登 录</el-button
        >
      </el-form-item>
    </el-form>
    <div class="extra-warp">
      <div class="t-cursor link-text" @click="$emit('switchToForget')">忘记密码?</div>
      <div class="t-cursor link-text" @click="$emit('switchToRegister')">立即注册</div>
    </div>
  </div>
</template>

<script>
import { login } from "@/api/user";
import { Encrypt } from "@/utils/secret";
export default {
  components: {},
  data() {
    return {
      ruleFormRef: null,
      ruleForm: {
        identity: "",
        password: "",
      },
      rules: {
        identity: [
          { required: true, message: "请输入用户名", trigger: "blur" },
        ],
        password: [{ required: true, message: "请输入密码", trigger: "blur" }],
      },
      loading: false
    };
  },
  mounted() {},
  methods: {
    login() {
      this.$refs.ruleFormRef.validate((valid) => {
        if (valid) {
          try {
            this.loading = true;
            // 对密码进行加密
            const password = Encrypt(this.ruleForm.password);
            login({...this.ruleForm, password}).then(res => {
              if (res) {
                // 登录成功，保存用户信息到Vuex
                const userInfo = {
                  ...res,
                  password: Encrypt(res.password)
                };
                
                // 先保存到Vuex
                this.$store.dispatch('setUserInfo', userInfo);
                
                // 根据角色跳转到不同页面
                if (this.$store.state.user.userInfo.role === 0) {
                  this.$router.push({path: '/home'});
                } else {
                  this.$router.push({path: '/home/diagnosisList'});
                }
              }
            }).catch(error => {
              console.error('登录失败:', error);
              this.$message.error('登录失败，请检查用户名和密码');
            }).finally(() => {
              this.loading = false;
            });
          } catch (error) {
            console.error('登录过程中发生错误:', error);
            this.$message.error('登录过程中发生错误');
            this.loading = false;
          }
        } else {
          return false;
        }
      });
    },
  },
};
</script>
<style lang="scss" scoped>
.login-form-container {
  width: 100%;
}

.submit-btn {
  width: 100%;
  letter-spacing: 8px;
  font-weight: 500;
  margin-top: 10px;
  height: 40px;
  font-size: 16px;
  background-color: var(--base-color);
  border-color: var(--base-color);
  transition: all 0.3s;
  
  &:hover, &:focus {
    background-color: darken(#a8824a, 5%);
    border-color: darken(#a8824a, 5%);
    transform: translateY(-2px);
    box-shadow: 0 5px 15px rgba(168, 130, 74, 0.3);
  }
  
  &:active {
    transform: translateY(0);
  }
}

.extra-warp {
  display: flex;
  justify-content: space-between;
  margin-top: 15px;
  font-size: 14px;
  color: #686b70;
}

.link-text {
  transition: all 0.3s;
  position: relative;
  
  &:hover {
    color: var(--base-color);
  }
  
  &::after {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 0;
    width: 0;
    height: 1px;
    background-color: var(--base-color);
    transition: width 0.3s;
  }
  
  &:hover::after {
    width: 100%;
  }
}

:deep(.el-input--prefix .el-input__inner) {
  padding-left: 30px;
  height: 40px;
  transition: all 0.3s;
  
  &:focus {
    border-color: var(--base-color);
    box-shadow: 0 0 0 2px rgba(168, 130, 74, 0.2);
  }
}

:deep(.el-input__prefix) {
  left: 10px;
}

.el-form-item {
  margin-bottom: 15px;
}

:deep(.el-form-item__error) {
  padding-top: 4px;
}

</style>
