import Vue from 'vue';
import VueRouter from 'vue-router';
import store from '@/store/index';
import Api from '@/apis/api';

Vue.use(VueRouter);

const notLoggedIn = () => (form, to, next) => {
  if (store.state.user.accessToken) {
    next('/user');
  }
  next();
};

const routes = [
  {
    path: '/',
    name: 'Login',
    beforeEnter: notLoggedIn(),
    component: () => import('@/views/login/Login'),
    meta: { unauthorized: true },
  },
  {
    path: '/initial',
    component: () => import('@/views/Main.vue'),
    children: [{ path: '', component: () => import('@/views/login/InitialPwdSetting') }],
  },
  {
    path: '/user',
    component: () => import('@/views/Main.vue'),
    children: [
      { path: '', component: () => import('@/views/user/List') },
      { path: 'modify/:userId', component: () => import('@/views/user/Modify') },
    ],
  },
  {
    path: '/patient',
    component: () => import('@/views/Main.vue'),
    children: [
      { path: '', component: () => import('@/views/patient/List') },
      { path: 'register', component: () => import('@/views/patient/Register') },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/views/Main.vue'),
    children: [
      { path: '', component: () => import('@/views/admin/List') },
      { path: 'register', component: () => import('@/views/admin/Register') },
    ],
  },
  {
    path: '/statistics',
    component: () => import('@/views/Main.vue'),
    children: [
      {
        path: '',
        redirect: 'period',
        component: () => import('@/views/statistics/Statistics'),
        children: [{ path: 'period', component: () => import('@/views/statistics/PeriodStatistics') }],
      },
    ],
  },
  {
    path: '*',
    redirect: '/',
  },
];

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes,
});

router.beforeEach(async (to, from, next) => {
  // 로그인 이후 새로고침 시
  if (!store.state.user.accessToken && localStorage.getItem('accessToken')) {
    const res = await Api.autoLogin();
    // res.data : 서버 통신 성공
    if (res && res.data) {
      // res.data.rt === 200 : 통신 이후 정상 상태 확인 성공
      if (res.data.rt === 200) {
        // 유저 정보 업데이트
        store.dispatch('user/updateInfo', res.data.response);
      } else {
        alert('알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요. 오류 코드 : ' + res.data.rt);
      }
    } else {
      // 서버 통신 실패 시 정상 페이지 이동, 해당 페이지 내 API 통신에서 서버 오류 alert
      return next();
    }
  }

  // 인증이 필요한 페이지에서 토큰이 없는 경우 로그인 페이지로 이동
  if (!to.matched.some(record => record.meta.unauthorized) && !store.state.user.accessToken) {
    return next('/');
  }

  next();
});

export default router;
