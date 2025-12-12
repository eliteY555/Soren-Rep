// pages/profile/change-password.js
const api = require('../../utils/api.js');
const util = require('../../utils/util.js');

Page({
  data: {},

  // 提交表单
  handleSubmit(e) {
    const formValue = e.detail.value;
    const { oldPassword, newPassword, confirmPassword } = formValue;
    
    // 校验旧密码
    if (!oldPassword || oldPassword.trim() === '') {
      util.showError('请输入旧密码');
      return;
    }
    
    // 校验新密码
    if (!newPassword || newPassword.trim() === '') {
      util.showError('请输入新密码');
      return;
    }
    
    if (newPassword.length < 6 || newPassword.length > 20) {
      util.showError('密码长度应为6-20个字符');
      return;
    }
    
    // 校验确认密码
    if (newPassword !== confirmPassword) {
      util.showError('两次输入的密码不一致');
      return;
    }
    
    // 校验新旧密码不能相同
    if (oldPassword === newPassword) {
      util.showError('新密码不能与旧密码相同');
      return;
    }
    
    // 提交修改
    util.showLoading('修改中...');
    
    api.changePassword({
      oldPassword: oldPassword,
      newPassword: newPassword
    }).then(res => {
      util.hideLoading();
      
      if (res.code === 200) {
        wx.showModal({
          title: '修改成功',
          content: '密码已修改，请重新登录',
          showCancel: false,
          success: () => {
            // 清除本地存储
            wx.removeStorageSync('token');
            wx.removeStorageSync('userInfo');
            
            // 跳转到登录页
            wx.reLaunch({
              url: '/pages/login/login'
            });
          }
        });
      }
    }).catch(err => {
      util.hideLoading();
      util.showError(err.msg || '修改失败');
    });
  }
});

