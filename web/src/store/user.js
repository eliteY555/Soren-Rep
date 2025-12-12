const userStore = {
  state: {
    userInfo: null,
  },
  mutations: {
    SET_USERINFO(state, userInfo) {
      // 确保userInfo是有效对象
      if (userInfo && typeof userInfo === 'object') {
      state.userInfo = userInfo; // 设置用户信息
        // 使用try-catch防止localStorage异常
        try {
      localStorage.setItem("userInfo", JSON.stringify(userInfo));
        } catch (error) {
          console.error('保存用户信息到localStorage失败:', error);
        }
      } else {
        console.warn('尝试设置无效的用户信息:', userInfo);
      }
    },
    CLEAR_USERINFO(state) {
      state.userInfo = null; // 清除用户信息
      try {
      localStorage.removeItem("userInfo");
      } catch (error) {
        console.error('从localStorage移除用户信息失败:', error);
      }
    },
    INIT_USERINFO(state) {
      try {
        const userInfoStr = localStorage.getItem("userInfo");
        if (userInfoStr) {
          const userInfo = JSON.parse(userInfoStr);
          if (userInfo && typeof userInfo === 'object' && userInfo.userId) {
            state.userInfo = userInfo;
          } else {
            // 无效的用户信息，清除它
            localStorage.removeItem("userInfo");
            state.userInfo = null;
          }
        }
      } catch (error) {
        console.error('从localStorage读取用户信息失败:', error);
        state.userInfo = null;
      }
    }
  },
  actions: {
    setUserInfo({ commit }, userInfo) {
      commit("SET_USERINFO", userInfo); // 调用 mutation 设置用户信息
    },
    clearUserInfo({ commit }) {
      commit("CLEAR_USERINFO"); // 调用 mutation 清除用户信息
    },
    initUserInfo({ commit }) {
      commit("INIT_USERINFO"); // 初始化用户信息
    }
  },
};

export default userStore;
