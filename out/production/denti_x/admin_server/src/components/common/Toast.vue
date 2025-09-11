<template>
  <v-snackbar v-model="toastStatus" :timeout="timeout" class="toast_wrap">
    <i class="icon success_icon"></i>
    <span>{{ content }}</span>
    <template #action="{ attrs }">
      <!--      <v-btn v-if="linkBtn" text v-bind="attrs" @click="$emit('nextPage')">{{ linkBtn }}</v-btn>-->
      <v-btn text v-bind="attrs" @click="toastStatus = false">확인</v-btn>
    </template>
  </v-snackbar>
</template>

<script>
export default {
  name: 'Toast',
  data() {
    return {
      toastStatus: false,
      content: '',
      linkBtn: '',
      timeout: 5000,
    };
  },
  methods: {
    show(data) {
      this.content = data.content || '완료 되었습니다.';
      this.linkBtn = data.linkBtn || '';
      this.timeout = data.timeout || 5000;
      this.toastStatus = true;
    },
  },
};
</script>

<style scoped></style>
