<template>
  <v-container fluid class="d-flex justify-center pa-0 flex-wrap" fill-height>
    <v-card v-if="!password" width="900" class="register_wrap">
      <h1>관리자계정 등록</h1>
      <validation-observer ref="observer" v-slot="{ invalid }">
        <form @submit.prevent="submit">
          <validation-provider v-slot="{ errors }" name="이름" :rules="{ required: true, checkName: /^[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z]*$/ }">
            <label>
              관리자 이름
              <v-text-field
                v-model="form.adminName"
                class="mt-2 mb-4"
                background-color="inputBG"
                outlined
                :error-messages="errors"
                height="48"
                placeholder="관리자 이름을 입력하세요."
                dense
                required
                @input="errorMsg = ''"></v-text-field>
            </label>
          </validation-provider>
          <validation-provider v-slot="{ errors }" name="아이디" :rules="{ required: true }">
            <label>
              아이디
              <v-text-field
                v-model="form.adminLoginIdentifier"
                class="mt-2 mb-4"
                background-color="inputBG"
                outlined
                :error-messages="errors"
                height="48"
                placeholder="아이디를 입력하세요."
                dense
                required
                @input="errorMsg = ''"></v-text-field>
            </label>
          </validation-provider>
          <validation-provider v-slot="{ errors }" name="연락처" :rules="{ required: true, checkPhoneNumber: /^01([0|1|6|7|8|9])-?([0-9]{3,4})-?([0-9]{4})$/ }">
            <label>
              연락처
              <span class="guide_text">동명이인 구분을 위해 개인연락처를 입력해주세요.</span>
              <v-text-field
                v-model="form.adminPhoneNumber"
                class="mt-2"
                background-color="inputBG"
                outlined
                :error-messages="errors"
                height="48"
                placeholder="연락처를 입력하세요."
                dense
                required
                @input="errorMsg = ''"></v-text-field>
            </label>
          </validation-provider>
          <span v-if="errorMsg" class="error--text error_msg">{{ errorMsg }}</span>
          <div class="d-flex justify-end mt-6">
            <v-btn color="primary" class="submit_btn" width="180" height="48" :disabled="invalid" @click="submit">신규 등록</v-btn>
          </div>
        </form>
      </validation-observer>
    </v-card>

    <v-card v-else width="900" class="register_wrap">
      <h1 class="mb-12">관리자계정 등록 완료</h1>
      <div class="register_info_wrap">
        <p class="info_top_txt mb-5">아래 계정 정보를 담당자에게 전달해주세요.</p>
        <div class="info_box px-13 py-8 mb-7">
          <dl class="d-flex mb-7">
            <dt>회원아이디</dt>
            <dd>{{ form.adminLoginIdentifier }}</dd>
          </dl>
          <dl class="d-flex">
            <dt>임시비밀번호</dt>
            <dd>{{ password }}</dd>
          </dl>
        </div>
        <p class="info_bottom_txt ma-0">
          ① 첫 로그인시 자동으로 비밀번호 변경 페이지로 이동합니다.
          <br />
          ② 해당 관리자에게 반드시 비밀번호를 변경 후 사용 할 수 있게 안내해주세요.
          <br />
          ※ 정보 신뢰도를 위해 계정을 공유해서 사용하는 것을 금지해주세요.
        </p>
      </div>
      <div class="d-flex justify-end">
        <v-btn color="primary" class="submit_btn" width="180" height="48" to="/admin">확인</v-btn>
      </div>
    </v-card>
  </v-container>
</template>

<script>
import { required, regex } from 'vee-validate/dist/rules';
import { extend, ValidationObserver, ValidationProvider, setInteractionMode } from 'vee-validate';
import Api from '@/apis/api';
import router from '@/router';
import { mapState } from 'vuex';

setInteractionMode('eager');

extend('required', {
  ...required,
  message: '{_field_} 필드를 입력해주세요.',
});
extend('checkName', {
  ...regex,
  message: '특수문자, 숫자, 공백을 제외하고 입력해주세요.',
});
extend('checkPhoneNumber', {
  ...regex,
  message: '올바른 연락처를 입력해주세요.',
});
export default {
  name: 'Register',
  components: {
    ValidationProvider,
    ValidationObserver,
  },
  data: () => ({
    form: {
      adminName: '',
      adminLoginIdentifier: '',
      adminPhoneNumber: '',
    },
    errorMsg: '',
    password: '',
  }),
  computed: {
    ...mapState('user', ['adminIsSuper']),
  },
  watch: {
    adminIsSuper() {
      if (this.adminIsSuper) {
        this.checkAccess();
      }
    },
  },
  created() {
    if (this.adminIsSuper) {
      this.checkAccess();
    }
  },
  methods: {
    checkAccess() {
      if (this.adminIsSuper !== 'Y') {
        alert('잘못된 접근입니다.');
        router.push('/').catch(() => {});
        return false;
      }
    },
    submit() {
      this.form.adminPhoneNumber = this.form.adminPhoneNumber.replace(/-/g, '');
      // 관리자 계정 생성
      Api.createAccount(this.form).then(res => {
        if (res && res.data.rt === 200) {
          this.password = res.data.response.adminPassword;
        } else {
          this.errorMsg = res.data.rtMsg;
        }
      });
    },
  },
};
</script>

<style scoped></style>
