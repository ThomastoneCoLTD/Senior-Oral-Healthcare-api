<template>
  <v-container fluid class="d-flex justify-center pa-0 flex-wrap container__wrap" fill-height>
    <v-card width="900" class="register_wrap">
      <h1>환자 등록</h1>
      <validation-observer ref="observer" v-slot="{ invalid }">
        <form @submit.prevent="submit">
          <validation-provider v-slot="{ errors }" name="이름" :rules="{ required: true, checkPatientName: /^(?!.* $)(?! .*$)[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z ]{2,100}$/ }">
            <label>
              환자 이름
              <v-text-field
                v-model="form.patientName"
                class="mt-2 mb-4"
                background-color="inputBG"
                outlined
                :error-messages="errors"
                height="48"
                placeholder="이름을 입력해 주세요."
                dense
                required
                @input="errorMsg = ''"></v-text-field>
            </label>
          </validation-provider>
          <validation-provider v-slot="{ errors }" name="연락처" :rules="{ required: true, checkPatientPhoneNumber: /^([0-9]{11})$/ }">
            <label>
              연락처
              <v-text-field
                v-model="form.patientPhoneNumber"
                class="mt-2 mb-4"
                background-color="inputBG"
                outlined
                :error-messages="errors"
                height="48"
                placeholder="연락처를 입력해 주세요. (- 없이 숫자만)"
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
  </v-container>
</template>

<script>
import { regex, required } from 'vee-validate/dist/rules';
import { extend, ValidationObserver, ValidationProvider, setInteractionMode } from 'vee-validate';
import Api from '@/apis/api';
import router from '@/router';

setInteractionMode('eager');

extend('required', {
  ...required,
  message: '{_field_} 필드를 입력해 주세요.',
});
extend('checkPatientName', {
  ...regex,
  message: '이름은 앞뒤 공백 없이 한글이나 영문으로 2자 이상입니다.',
});
extend('checkPatientPhoneNumber', {
  ...regex,
  message: '올바른 연락처를 입력해 주세요.',
});
export default {
  name: 'Register',
  components: {
    ValidationProvider,
    ValidationObserver,
  },
  data: () => ({
    form: {
      patientName: '',
      patientPhoneNumber: '',
    },
    errorMsg: '',
  }),
  methods: {
    submit() {
      Api.createPatient(this.form).then(res => {
        if (res && res.data.rt === 200) {
          this.$root.toast.show({
            content: `환자 등록이 완료되었습니다.`,
          });
          router.push('/patient');
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
