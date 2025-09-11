<template>
  <div class="search_row mb-5 px-8">
    <p>{{ title }}</p>
    <v-row no-gutters class="date_filter">
      <v-col class="d-flex pr-4" cols="6">
        <button type="button" class="active_date" :class="{ active: value.active === 'TODAY' }" @click="dateSet('TODAY')">오늘</button>
        <button type="button" class="active_date" :class="{ active: value.active === 'WEEK1' }" @click="dateSet('WEEK1')">1주일</button>
        <button type="button" class="active_date" :class="{ active: value.active === 'MONTH1' }" @click="dateSet('MONTH1')">1개월</button>
        <button type="button" class="active_date" :class="{ active: value.active === 'MONTH3' }" @click="dateSet('MONTH3')">3개월</button>
        <button type="button" class="active_date" :class="{ active: value.active === 'YEAR1' }" @click="dateSet('YEAR1')">1년</button>
        <button type="button" class="active_date" :class="{ active: value.active === 'ALL' }" @click="dateSet('ALL')">전체</button>
      </v-col>
      <v-col class="d-flex" cols="6">
        <date-picker v-model="selectedDate.startDate" :max="selectedDate.endDate" @fieldChanged="removeActiveClass"></date-picker>
        <span class="mark">~</span>
        <date-picker v-model="selectedDate.endDate" :min="selectedDate.startDate" @fieldChanged="removeActiveClass"></date-picker>
      </v-col>
    </v-row>
  </div>
</template>

<script>
import DatePicker from '@/components/common/DatePicker';
export default {
  name: 'DateFilter',
  components: {
    DatePicker,
  },
  props: {
    value: {
      type: Object,
      default: () => {
        return {
          startDate: '',
          endDate: '',
          active: '',
        };
      },
    },
    title: {
      type: String,
      default: '',
    },
  },
  data: () => {
    return {
      calendarMenu1: false,
      calendarMenu2: false,
      date: new Date().toISOString().substring(0, 10),
    };
  },
  computed: {
    selectedDate: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit('input', value);
      },
    },
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
  watch: {
    selectedDate: {
      handler(val) {
        if (val.startDate === '' && val.endDate === '') this.removeActiveClass();
      },
      deep: true,
    },
  },
  methods: {
    dateSet(period) {
      this.value.active = period;
      this.selectedDate.startDate = '';
      this.selectedDate.endDate = '';
      for (const key in this.activeObj) {
        if (this.activeObj[key] === period) {
          const dateSplit = key.split('_');
          this.selectedDate.startDate = dateSplit[0];
          this.selectedDate.endDate = dateSplit[1];
          return;
        }
      }
    },
    removeActiveClass() {
      if (!this.selectedDate.startDate && !this.selectedDate.endDate) {
        this.value.active = 'ALL';
      } else {
        this.value.active = '';
      }
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
