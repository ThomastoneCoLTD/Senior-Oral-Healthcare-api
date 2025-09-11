<template>
  <v-container fluid class="d-flex justify-center pa-0 flex-wrap" fill-height>
    <v-card width="900" class="initial_setting_wrap">
      <h1>
        비밀번호 변경
        <span>(새 비밀번호 입력)</span>
      </h1>
      <form>
        <label>
          새 비밀번호
          <v-text-field v-model="password" type="password" class="mt-2 mb-2" background-color="inputBG" outlined height="48" placeholder="새 비밀번호를 입력하세요." dense required></v-text-field>
        </label>
        <label>
          비밀번호 확인
          <v-text-field
            v-model="confirmPassword"
            type="password"
            class="mt-2 mb-2"
            background-color="inputBG"
            outlined
            :error-messages="confirmPassword && password !== confirmPassword ? ['비밀번호가 일치하지 않습니다. 다시 시도해주세요.'] : ''"
            height="48"
            placeholder="새 비밀번호 확인"
            dense
            required></v-text-field>
        </label>
        <v-btn color="primary" class="submit_btn mt-10" width="100%" height="60" :disabled="!password || !confirmPassword || password !== confirmPassword" @click="submit">비밀번호 변경하기</v-btn>
      </form>
    </v-card>
  </v-container>
</template>

<script>
import Api from '@/apis/api';
import { mapState } from 'vuex';

export default {
  name: 'InitialPwdSetting',
  data: () => ({
    password: '',
    confirmPassword: '',
  }),
  computed: {
    ...mapState('user', ['adminId']),
  },
  methods: {
    submit() {
      const form = {
        adminPassword: this.password,
      };
      Api.passwordModify(form).then(res => {
        if (res && res.data.rt === 200) {
          this.$router.push('/user');
        } else {
          if (res.data.rt === 417) {
            this.$root.alert.show({
              content: res.data.rtMsg,
            });
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
