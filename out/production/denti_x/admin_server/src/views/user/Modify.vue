<template>
  <v-container fluid class="d-flex justify-center pa-0 flex-wrap container__wrap" fill-height>
    <v-card width="900" class="register_wrap">
      <h1>사용자 수정</h1>
      <validation-observer ref="observer" v-slot="{ invalid }">
        <form @submit.prevent="submit">
          <validation-provider v-slot="{ errors }" name="아이디" :rules="{ required: true, checkUserLoginIdentifier: /^[a-zA-Z0-9]{4,12}$/ }">
            <label>
              아이디
              <v-text-field
                v-model="form.userLoginIdentifier"
                class="mt-2 mb-4"
                background-color="inputBG"
                outlined
                :error-messages="errors"
                height="48"
                placeholder="아이디를 입력해 주세요."
                dense
                required
                @input="errorMsg = ''"></v-text-field>
            </label>
          </validation-provider>
          <validation-provider v-slot="{ errors }" name="닉네임" :rules="{ required: true, checkUserName: /^(?!.* $)(?! .*$)[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z ]{2,100}$/ }">
            <label>
              닉네임
              <v-text-field
                v-model="form.userName"
                class="mt-2 mb-4"
                background-color="inputBG"
                outlined
                :error-messages="errors"
                height="48"
                placeholder="닉네임을 입력해 주세요."
                dense
                required
                @input="errorMsg = ''"></v-text-field>
            </label>
          </validation-provider>
          <validation-provider v-slot="{ errors }" name="성별">
            <label>
              성별
              <v-select
                v-model="form.userGender"
                class="mt-2"
                :items="genderStatus"
                item-text="text"
                item-value="value"
                :error-messages="errors"
                :menu-props="{ bottom: true, offsetY: true }"
                background-color="inputBG"
                height="48"
                placeholder="성별 선택"
                item-color="selectedBlue"
                dense
                required
                outlined
                @input="errorMsg = ''">
                <template #append>
                  <i class="icon select_menu_icon"></i>
                </template>
              </v-select>
            </label>
          </validation-provider>
          <span v-if="errorMsg" class="error--text error_msg">{{ errorMsg }}</span>
          <div class="d-flex justify-end mt-6">
            <v-btn color="primary" class="submit_btn" width="180" height="48" :disabled="invalid" @click="submit">정보 수정</v-btn>
          </div>
        </form>
      </validation-observer>
    </v-card>
  </v-container>
</template>

<script>
import { regex, required } from 'vee-validate/dist/rules';
import { extend, ValidationObserver, ValidationProvider, setInteractionMode } from 'vee-validate';
import Api from '@/apis/api';
import router from '@/router';
import { mapState } from 'vuex';

setInteractionMode('eager');

extend('required', {
  ...required,
  message: '{_field_} 필드를 입력해 주세요.',
});
extend('checkUserLoginIdentifier', {
  ...regex,
  message: '아이디는 숫자나 영문으로 4~12자입니다.',
});
extend('checkUserName', {
  ...regex,
  message: '닉네임은 앞뒤 공백 없이 한글이나 영문으로 2자 이상입니다.',
});
export default {
  name: 'Register',
  components: {
    ValidationProvider,
    ValidationObserver,
  },
  data: () => ({
    form: {
      userId: '',
      userLoginIdentifier: '',
      userName: '',
      userGender: '',
    },
    errorMsg: '',
  }),
  computed: {
    ...mapState('commonValue', ['genderStatus']),
  },
  created() {
    this.form.userId = this.$route.params.userId;
    Api.getUser({ userId: this.form.userId }).then(res => {
      if (res && res.data.rt === 200) {
        for (const key in res.data.response) {
          this.form[key] = res.data.response[key];
        }
      } else {
        if (res.data.rt === 416) {
          alert(res.data.rtMsg);
          this.$router.push('/user');
          return;
        }
        this.$root.alert.show({
          content: `알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요. 오류 코드 : ${res.data.rt}`,
        });
      }
    });
  },
  methods: {
    submit() {
      Api.modifyUser(this.form).then(res => {
        if (res && res.data.rt === 200) {
          this.$root.toast.show({
            content: `정보 수정이 완료되었습니다.`,
          });
          router.push('/user');
        } else {
          if (res.data.rt === 422) {
            this.errorMsg = res.data.rtMsg;
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
