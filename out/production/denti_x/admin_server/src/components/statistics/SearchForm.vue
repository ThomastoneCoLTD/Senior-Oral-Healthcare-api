<template>
  <form ref="searchForm" @submit.prevent="submit">
    <date-filter v-model="selectedDate" title="기간 설정"></date-filter>
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
import DateFilter from '@/components/statistics/DateFilter';
import { mapState } from 'vuex';

export default {
  name: 'SearchForm',
  components: {
    SelectBox,
    DateFilter,
  },
  data: () => ({
    form: {
      userIdentifierOrName: '',
      corId: '',
      oralCheckResultTotalType: '',
      userGender: '',
      isVerify: '',
      startDate: '',
      endDate: '',
    },
    selectedDate: {
      startDate: '',
      endDate: '',
      active: '',
    },
  }),
  computed: {
    ...mapState('commonValue', ['oralCheckType', 'genderStatus', 'verifyStatus']),
    activeObj() {
      const today = new Date();
      let baseDate;
      baseDate = new Date();
      baseDate.setDate(baseDate.getDate() - 6);

      const form = {};
      form[`${this.getDate(today)}_${this.getDate(today)}`] = 'TODAY';

      baseDate = new Date();
      baseDate.setDate(baseDate.getDate() - 6);
      form[`${this.getDate(baseDate)}_${this.getDate(today)}`] = 'WEEK1';

      baseDate = new Date();
      baseDate.setDate(baseDate.getDate() + 1);
      baseDate.setMonth(baseDate.getMonth() - 1);
      form[`${this.getDate(baseDate)}_${this.getDate(today)}`] = 'MONTH1';

      baseDate = new Date();
      baseDate.setDate(baseDate.getDate() + 1);
      baseDate.setMonth(baseDate.getMonth() - 3);
      form[`${this.getDate(baseDate)}_${this.getDate(today)}`] = 'MONTH3';

      baseDate = new Date();
      baseDate.setDate(baseDate.getDate() + 1);
      baseDate.setFullYear(baseDate.getFullYear() - 1);
      form[`${this.getDate(baseDate)}_${this.getDate(today)}`] = 'YEAR1';

      return form;
    },
  },
  created() {
    Object.assign(this.form, this.$route.query);
    if (this.$route.query.corId) this.form.corId = Number(this.$route.query.corId);
    this.selectedDate.startDate = this.form.startDate;
    this.selectedDate.endDate = this.form.endDate;
    if (this.form.startDate && this.form.endDate) {
      this.selectedDate.active = this.activeObj[`${this.form.startDate}_${this.form.endDate}`] || '';
    } else if (!this.selectedDate.startDate && !this.selectedDate.endDate) {
      this.selectedDate.active = 'ALL';
    }
  },
  methods: {
    reset() {
      let self = this;
      Object.keys(this.form).forEach(function (key) {
        self.form[key] = '';
      });
      Object.keys(this.selectedDate).forEach(function (key) {
        self.selectedDate[key] = '';
      });
      this.$emit('reset');
    },
    submit() {
      this.form = Object.assign(this.form, this.selectedDate);
      delete this.form.page; // 검색 시 1페이지로 초기화
      delete this.form.active; // active 설정 불필요
      this.$emit('search', this.form);
    },
    getDate(date) {
      const monthInt = date.getMonth() + 1;
      const month = monthInt < 10 ? `0${monthInt}` : monthInt;
      const day = date.getDate() < 10 ? `0${date.getDate()}` : date.getDate();
      return `${date.getFullYear()}-${month}-${day}`;
    },
  },
};
</script>

<style scoped></style>
