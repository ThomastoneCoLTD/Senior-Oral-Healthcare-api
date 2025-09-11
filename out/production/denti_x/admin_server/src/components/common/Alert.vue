<template>
  <v-dialog v-model="alertStatus" max-width="440" @keydown.enter.prevent="alertStatus = false">
    <v-card tile rounded="false" class="popup_wrap">
      <v-toolbar flat height="54">
        <i class="icon mr-2" :class="`${data.type}_icon`"></i>
        <v-toolbar-title>
          {{ data.headerText }}
        </v-toolbar-title>
        <v-spacer></v-spacer>
        <v-btn icon @click="alertStatus = false">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </v-toolbar>
      <v-card-text class="px-10 py-12">
        <slot name="body">
          <div class="text_box" :class="{ 'text-center': !data.center }">{{ data.content }}</div>
        </slot>
      </v-card-text>
      <v-card-actions class="mr-2 pb-4">
        <v-btn color="selectedBlue" width="100%" height="48" class="white--text" @click="alertStatus = false">{{ data.btnText }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  name: 'Alert',
  data() {
    return {
      alertStatus: false,
      data: {
        type: 'error',
        headerText: '알림',
        content: '오류입니다.',
        btnText: '확인',
        center: true,
      },
    };
  },
  methods: {
    show(data) {
      for (const key in this.data) {
        if (data[key]) this.data[key] = data[key];
      }
      this.alertStatus = true;
    },
  },
};
</script>

<style scoped></style>
