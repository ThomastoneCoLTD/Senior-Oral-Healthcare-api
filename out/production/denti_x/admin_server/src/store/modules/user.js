import router from '@/router';

const getDefaultState = () => {
  return {
    accessToken: null,
    refreshToken: null,
    adminId: null,
    adminName: null,
    adminIsSuper: null,
  };
};

export default {
  namespaced: true,
  state: getDefaultState(),
  mutations: {
    resetUserState(state) {
      Object.assign(state, getDefaultState());
    },
    setLoginUserInfo(state, payload) {
      Object.assign(state, payload);
    },
    setAccessToken(state, payload) {
      state.accessToken = payload;
    },
  },
  actions: {
    updateInfo({ commit }, payload) {
      commit('setLoginUserInfo', payload);
      localStorage.setItem('accessToken', payload.accessToken);
      localStorage.setItem('refreshToken', payload.refreshToken);
    },
    logout({ commit }) {
      commit('resetUserState');
      localStorage.clear();
      router.push('/');
    },
  },
};
