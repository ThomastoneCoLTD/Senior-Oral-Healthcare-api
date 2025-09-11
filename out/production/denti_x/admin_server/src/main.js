import Vue from 'vue';
import App from '@/App';
import router from '@/router';
import store from '@/store';
import vuetify from '@/plugins/vuetify';
import VeeValidate from 'vee-validate';
import VueCookies from 'vue-cookies';
import common from '@/plugins/common';
import VueHtml2Canvas from 'vue-html2canvas/src';

Vue.config.productionTip = false;
Vue.use(VueCookies);
Vue.use(common);
Vue.use(VueHtml2Canvas);

new Vue({
  router,
  store,
  vuetify,
  VeeValidate,
  render: h => h(App),
}).$mount('#app');
