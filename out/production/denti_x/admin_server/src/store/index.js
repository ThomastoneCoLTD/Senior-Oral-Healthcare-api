import Vue from 'vue';
import Vuex from 'vuex';
import user from '@/store/modules/user';
import table from '@/store/modules/table';
import commonValue from '@/store/modules/commonValue';

Vue.use(Vuex);

export default new Vuex.Store({
  modules: {
    user,
    table,
    commonValue,
  },
});
