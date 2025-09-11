<template>
  <div>
    <v-card-text class="px-10 pb-5 pt-10 text-center deletion_info">
      <p>{{ userType === 'admin' ? data.adminName : userType === 'patient' ? data.patientName : data.userName }} 님을 삭제하시겠습니까?</p>
      <div v-html="content"></div>
    </v-card-text>
    <div class="deletion_input_wrap px-10">
      <p>삭제 대상자</p>
      <div>
        <v-text-field v-model="deletePopupText" background-color="inputBG" outlined height="48" placeholder="이름을 입력하세요" dense required></v-text-field>
      </div>
    </div>
    <v-card-actions class="mr-2 pb-4 mt-8">
      <v-btn color="btnBlack" width="50%" height="48" class="white--text" @click="close">미동의</v-btn>
      <v-btn
        color="primary"
        width="50%"
        height="48"
        class="white--text"
        :disabled="deletePopupText !== (userType === 'admin' ? data.adminName : userType === 'patient' ? data.patientName : data.userName)"
        @click="submit">
        동의함
      </v-btn>
    </v-card-actions>
  </div>
</template>

<script>
export default {
  name: 'DeletePopupForm',
  props: {
    content: {
      type: String,
      default: '삭제 하시겠습니까?',
    },
    data: {
      type: Object,
      default: () => {
        return {
          adminName: '',
          userName: '',
          patientName: '',
        };
      },
    },
    value: {
      type: String,
      default: '',
    },
    userType: {
      type: String,
      default: '',
    },
  },
  computed: {
    deletePopupText: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit('input', value);
      },
    },
  },
  methods: {
    close() {
      this.deletePopupText = '';
      this.$emit('close');
    },
    submit() {
      this.deletePopupText = '';
      this.$emit('submit', 'user_delete', this.data);
    },
  },
};
</script>

<style scoped></style>
