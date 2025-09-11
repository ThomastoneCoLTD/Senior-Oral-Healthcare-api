<template>
  <Doughnut :chart-options="chartOptions" :chart-data="chartData" />
</template>

<script>
import { Doughnut } from 'vue-chartjs/legacy';
import { Chart as ChartJS, Tooltip, ArcElement } from 'chart.js';

ChartJS.register(Tooltip, ArcElement);
export default {
  components: { Doughnut },
  props: {
    chartOptions: {
      type: Object,
      default: null,
    },
    chartData: {
      type: Object,
      default: null,
    },
  },
};
</script>

<style scoped></style>
