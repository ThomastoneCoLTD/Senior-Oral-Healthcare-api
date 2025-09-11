<template>
  <v-app>
    <v-app-bar app absolute color="primaryDark" dense height="60" class="px-8">
      <router-link to="/" exact class="logo_link">
        <img src="@/assets/img/common/logo.png" alt="로고" style="height: 40px" />
        <span class="blind">덴티엑스</span>
      </router-link>
      <div class="tab_wrap d-flex justify-space-between">
        <div class="left_tab_wrap d-flex justify-center">
          <router-link v-for="tab in tabs" :key="tab.id" active-class="active" :to="tab.route" class="white--text tab_item d-flex align-center justify-center">
            {{ tab.name }}
          </router-link>
        </div>
        <div class="right_tab_wrap d-flex justify-center align-center">
          <router-link to="/patient/register" class="register_user_link white--text mr-6 d-flex justify-center">
            <v-icon class="mr-1">mdi-plus</v-icon>
            환자 등록
          </router-link>
          <div class="user_menu_wrap">
            <v-menu bottom offset-y transition="slide-y-transition">
              <template #activator="{ on, attrs }">
                <button class="menu_btn d-flex white--text" v-bind="attrs" v-on="on">
                  <span class="text_ellipsis">{{ adminName }}</span>
                  님
                </button>
              </template>
              <v-list class="menu_list">
                <template v-if="adminIsSuper === 'Y'">
                  <v-list-item exact to="/admin" class="v-list-item--link">관리자 목록</v-list-item>
                  <v-list-item exact to="/admin/register" class="v-list-item--link">관리자 등록</v-list-item>
                </template>
                <v-list-item exact to="/initial" class="v-list-item--link">비밀번호 변경</v-list-item>
                <v-list-item class="v-list-item--link" @click.prevent="$store.dispatch('user/logout')">로그아웃</v-list-item>
              </v-list>
            </v-menu>
          </div>
        </div>
      </div>
    </v-app-bar>
    <v-main class="main_wrap">
      <router-view :key="$route.fullPath"></router-view>
    </v-main>
    <v-footer app absolute color="darkGrey" class="align-center justify-center footer" padless>
      <v-card color="darkGrey" max-width="1360" width="1360" class="py-10">
        <v-card-text class="white--text pa-0 pb-5 1rem company_txt" cols="12">(주)카이아이컴퍼니</v-card-text>
        <v-card-text class="white--text pa-0 d-flex justify-space-between footer_sub_txt">
          <p class="ma-0">
            대표자 정호정 ㅣ사업자등록번호 568-88-00197
            <br />
            서울특별시 금천구 가산디지털1로 204, 12층 2~6호
          </p>
          <p class="ma-0">Copyright 2023 Kai-i co.,ltd all rights reserved.</p>
        </v-card-text>
      </v-card>
    </v-footer>
  </v-app>
</template>

<script>
import { mapState } from 'vuex';

export default {
  name: 'Main',
  data: () => ({
    tabs: [
      { name: '사용자 검색', route: `/user` },
      { name: '환자 목록', route: `/patient` },
      { name: '통계', route: `/statistics` },
    ],
  }),
  computed: {
    ...mapState('user', ['accessToken', 'adminName', 'adminIsSuper']),
  },
};
</script>

<style scoped></style>
