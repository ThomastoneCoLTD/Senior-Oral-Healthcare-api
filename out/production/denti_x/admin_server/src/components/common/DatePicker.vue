<template>
  <v-menu ref="menu" v-model="calendarStatus" class="calendar_wrap" :close-on-content-click="false" :return-value.sync="date" transition="scale-transition" offset-y min-width="auto">
    <template #activator="{ on, attrs }">
      <template v-if="custom === 'calendar'">
        <v-text-field v-model="dateText" flat solo :background-color="backgroundColor" :height="height" dense readonly v-bind="attrs" v-on="on">
          <template #append>
            <i class="icon select_menu_icon"></i>
          </template>
        </v-text-field>
      </template>
      <template v-else>
        <v-text-field
          v-model="selectedDate"
          :background-color="backgroundColor"
          outlined
          :height="height"
          dense
          placeholder="YYYY-MM-DD"
          append-icon="mdi-calendar-today"
          readonly
          v-bind="attrs"
          v-on="on"></v-text-field>
      </template>
    </template>
    <template v-if="allowedDates.length === 0">
      <v-date-picker
        v-model="selectedDate"
        :first-day-of-week="0"
        no-title
        scrollable
        :min="min"
        :max="max"
        :type="type"
        prev-icon="mdi-menu-left"
        next-icon="mdi-menu-right"
        color="selectedBlue"
        :day-format="setDay"
        @input="calendarStatus = false"></v-date-picker>
    </template>
    <template v-else>
      <v-date-picker
        v-model="selectedDate"
        :first-day-of-week="0"
        no-title
        scrollable
        :min="min"
        :max="max"
        :type="type"
        prev-icon="mdi-menu-left"
        next-icon="mdi-menu-right"
        color="selectedBlue"
        :allowed-dates="setAllowedDates"
        :day-format="setDay"
        @update:picker-date="pickerUpdate($event)"
        @input="calendarStatus = false"></v-date-picker>
    </template>
  </v-menu>
</template>

<script>
export default {
  name: 'DatePicker',
  props: {
    value: {
      type: String,
      default: '',
    },
    min: {
      type: String,
      default: '',
    },
    max: {
      type: String,
      default: '',
    },
    type: {
      type: String,
      default: 'date',
    },
    custom: {
      type: String,
      default: '',
    },
    height: {
      type: String,
      default: '48',
    },
    backgroundColor: {
      type: String,
      default: 'inputBG',
    },
    textType: {
      type: String,
      default: 'kr',
    },
    name: {
      type: String,
      default: '',
    },
    allowedDates: {
      type: Array,
      default: () => [],
    },
  },
  data: () => {
    return {
      calendarStatus: false,
      date: new Date().toISOString().substring(0, 10),
      dateText: '',
      availableDates: [],
    };
  },
  computed: {
    selectedDate: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit('clickDate', value);
        this.$emit('fieldChanged');
        this.$emit('input', value);
      },
    },
  },
  watch: {
    selectedDate(val) {
      this.dateText = this.convertDate(val);
    },
  },
  created() {
    this.dateText = this.value ? this.convertDate(this.value) : '';
  },
  methods: {
    setDay(date) {
      let i = new Date(date).getDate();
      return i;
    },
    convertDate(val) {
      if (this.textType === 'kr') {
        return this.$convertDateFormat('YYYYMMKR', val);
      } else {
        return `${this.$convertDateFormat('dot', val)} ${this.name}`;
      }
    },
    setAllowedDates(val) {
      return this.availableDates.includes(val);
    },
    pickerUpdate: function (yearMonth) {
      const availableDates = [];
      const thisMonth = this.allowedDates.filter(obj => obj.day.substring(0, 7) === yearMonth);
      for (const obj of thisMonth) {
        if (obj.state) {
          availableDates.push(obj.day);
        }
      }

      this.availableDates = availableDates;
    },
  },
};
</script>

<style scoped></style>
