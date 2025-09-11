<template>
  <Line :options="options" :chart-data="chartData" />
</template>

<script>
import { Line } from 'vue-chartjs/legacy';
import { Chart as ChartJS, Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale } from 'chart.js';

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale);

export default {
  components: { Line },
  props: {
    options: {
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
