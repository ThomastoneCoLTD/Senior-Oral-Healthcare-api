<template>
  <form ref="searchForm" @submit.prevent="submit">
    <div class="search_row mb-5 px-8">
      <p>검색어</p>
      <v-row no-gutters>
        <v-col class="pr-4" cols="3">
          <v-text-field
            v-model="form.patientNameOrPhoneNumber"
            background-color="inputBG"
            outlined
            height="48"
            append-icon="mdi-magnify"
            placeholder="이름 혹은 연락처"
            dense
            @keydown.enter.prevent="submit"></v-text-field>
        </v-col>
      </v-row>
    </div>
    <div class="pt-5 px-8 submit_btn_row d-flex justify-end">
      <v-btn color="white" height="44" class="reset_btn mr-4 d-flex justify-center align-center" @click="reset">
        <i class="icon reset mr-1"></i>
        초기화
      </v-btn>
      <v-btn color="primaryDark" height="44" class="submit_btn" @click="submit">검색</v-btn>
    </div>
  </form>
</template>

<script>
import SelectBox from '@/components/common/SelectBox';

export default {
  name: 'SearchForm',
  components: {
    SelectBox,
  },
  data: () => ({
    form: {
      patientNameOrPhoneNumber: '',
    },
  }),
  created() {
    Object.assign(this.form, this.$route.query);
  },
  methods: {
    reset() {
      let self = this;
      Object.keys(this.form).forEach(function (key) {
        self.form[key] = '';
      });
      this.$emit('reset');
    },
    submit() {
      this.form = Object.assign(this.form, this.selectedDate);
      delete this.form.page; // 검색 시 1페이지로 초기화
      this.$emit('search', this.form);
    },
  },
};
</script>

<style scoped></style>
