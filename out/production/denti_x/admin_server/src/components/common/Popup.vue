<template>
  <v-dialog v-model="show" max-width="440">
    <v-card tile rounded="false" class="popup_wrap">
      <v-toolbar flat height="54">
        <i class="icon mr-2" :class="`${popup.type}_icon`"></i>
        <v-toolbar-title>
          {{ popup.header }}
        </v-toolbar-title>
        <v-spacer></v-spacer>
        <v-btn icon @click="$emit('close')">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </v-toolbar>
      <slot name="special">
        <v-card-text class="px-10 py-12">
          <slot name="body">
            <div class="text_box" :class="{ 'text-center': !popup.center }" v-html="popup.content"></div>
          </slot>
        </v-card-text>
        <v-card-actions class="mr-2 pb-4">
          <template v-if="popup.btnType === '동의'">
            <v-btn color="btnBlack" width="50%" height="48" class="white--text" @click="$emit('close')">미동의</v-btn>
            <v-btn color="primary" width="50%" height="48" class="white--text" @click="$emit('submit', 'agree')">동의함</v-btn>
          </template>
          <template v-else-if="popup.btnType === '초기화'">
            <v-btn color="btnBlack" width="50%" height="48" class="white--text" @click="$emit('close')">취소</v-btn>
            <v-btn color="primary" width="50%" height="48" class="white--text" @click="$emit('submit', 'reset', popup.data)">초기화</v-btn>
          </template>
          <template v-else-if="popup.btnType === '삭제'">
            <v-btn color="btnBlack" width="50%" height="48" class="white--text" @click="$emit('close')">취소</v-btn>
            <v-btn color="error" width="50%" height="48" class="white--text" @click="$emit('submit', 'delete', popup.data)">삭제</v-btn>
          </template>
          <template v-else-if="popup.btnType === '인증하기'">
            <v-btn color="btnBlack" width="50%" height="48" class="white--text" @click="$emit('close')">취소</v-btn>
            <v-btn color="primary" width="50%" height="48" class="white--text" @click="$emit('submit', 'verify', popup.data)">인증하기</v-btn>
          </template>
          <template v-else>
            <v-btn color="selectedBlue" width="100%" height="48" class="white--text" @click="$emit('submit', 'confirm')">확인</v-btn>
          </template>
        </v-card-actions>
      </slot>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  name: 'Popup',
  props: {
    popup: {
      type: Object,
      default: () => {
        return {
          type: 'info',
          header: '제목',
          content: 'body slot 영역을 작성해주세요.',
          btnType: '확인',
          center: true,
          special: '',
          data: {},
        };
      },
    },
    value: Boolean,
  },
  computed: {
    show: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit('input', value);
      },
    },
  },
};
</script>

<style scoped></style>
