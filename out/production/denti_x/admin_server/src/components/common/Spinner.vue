<template>
  <v-dialog v-model="spinnerStatus" persistent width="300">
    <v-card color="primary" dark class="py-2">
      <v-card-text class="text-center white--text font-weight-bold">
        데이터를 불러오고 있습니다.
        <v-progress-linear indeterminate color="white" class="mb-0 mt-2"></v-progress-linear>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  name: 'Spinner',
  data() {
    return {
      spinnerStatus: false,
    };
  },
  methods: {
    set(status) {
      this.spinnerStatus = status;
    },
  },
};
</script>

<style scoped></style>
