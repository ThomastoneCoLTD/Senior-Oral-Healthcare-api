<template>
  <div class="no_data_wrap d-flex align-center justify-center" :style="{ height: `${height}px` }">
    <div>
      <div>
        <i class="icon no_files"></i>
      </div>
      <div class="text_box">데이터가 존재하지 않습니다.</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NoDataBox',
  props: {
    height: {
      type: Number,
      default: null,
    },
  },
};
</script>

<style scoped></style>
