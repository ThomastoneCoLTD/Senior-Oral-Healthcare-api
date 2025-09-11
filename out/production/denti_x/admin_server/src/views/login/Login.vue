<template>
  <v-app>
    <v-main class="login_wrap">
      <v-container fluid fill-height class="login_wrap pa-0">
        <div class="login_container">
          <h1 class="blind">로그인</h1>
          <div class="login_bg_wrap">
            <p class="bg_text">
              Hello,
              <br />
              WelcomeBack
            </p>
            <!--<p class="bg_logo">-->
            <!--  <span class="blind">덴티엑스</span>-->
            <!--</p>-->
          </div>
          <div class="login_form_wrap">
            <h3>
              <img src="@/assets/img/common/logo.png" alt="로고" />
              <b>관리자 SYSTEM</b>
            </h3>
            <form @submit.prevent="submit">
              <v-text-field
                v-model="form.adminLoginIdentifier"
                background-color="inputBG"
                outlined
                height="48"
                placeholder="아이디"
                dense
                required
                @keydown.enter.prevent="submit"
                @input="loginErrorMsg = false"></v-text-field>
              <v-text-field
                v-model="form.adminPassword"
                background-color="inputBG"
                outlined
                :append-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
                :type="showPassword ? 'text' : 'password'"
                placeholder="비밀번호"
                height="48"
                dense
                required
                @click:append="showPassword = !showPassword"
                @keydown.enter.prevent="submit"
                @input="loginErrorMsg = false"></v-text-field>
              <v-checkbox v-model="rememberLoginCheck" label="로그인 정보 기억" color="primary" class="mt-0" hide-details></v-checkbox>
              <span v-if="loginErrorMsg" class="error--text error_msg">입력하신 정보가 일치하지 않습니다. 다시 확인해 주세요.</span>
              <v-btn color="primary" class="submit_btn mt-16" width="100%" height="60" to="/" :disabled="form.adminLoginIdentifier === '' || form.adminPassword === ''" @click="submit">로그인</v-btn>
            </form>
          </div>
        </div>
      </v-container>
    </v-main>
  </v-app>
</template>

<script>
import Api from '@/apis/api';
import router from '@/router';

export default {
  name: 'Login',
  data: () => ({
    form: {
      adminLoginIdentifier: '',
      adminPassword: '',
    },
    showPassword: false,
    rememberLoginCheck: false,
    rememberIdKey: 'rememberId',
    loginErrorMsg: false,
  }),
  created() {
    if (this.$cookies.isKey(this.rememberIdKey)) {
      this.rememberLoginCheck = true;
      this.form.adminLoginIdentifier = this.$cookies.get(this.rememberIdKey);
    }
  },
  methods: {
    submit() {
      // 로그인 유효성 검사
      if (!this.form.adminLoginIdentifier || !this.form.adminPassword) {
        this.$root.alert.show({
          headerText: '필수항목 미입력',
          content: '아이디 또는 비밀번호를 입력해 주세요.',
        });
        return;
      }
      // 로그인 시도
      Api.login(this.form).then(res => {
        if (res && res.data.rt === 200) {
          // 로그인 정보 저장
          if (this.rememberLoginCheck) {
            this.$cookies.set(this.rememberIdKey, this.form.adminLoginIdentifier, '30d');
          } else {
            this.$cookies.remove(this.rememberIdKey);
          }
          // 로그인
          this.$store.dispatch('user/updateInfo', res.data.response);
          // 최초 1회 로그인 시 비밀번호 변경 페이지로 이동
          const page = res.data.response.isFirstLogin === 'Y' ? '/initial' : '/user';
          router.push(page);
        } else {
          if (res.data.rt === 401) {
            this.loginErrorMsg = true;
            return;
          }
          this.$root.alert.show({
            content: `알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요. 오류 코드 : ${res.data.rt}`,
          });
        }
      });
    },
  },
};
</script>

<style scoped></style>
