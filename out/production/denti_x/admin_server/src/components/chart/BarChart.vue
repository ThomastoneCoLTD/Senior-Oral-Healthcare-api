<template>
  <Bar :height="height" :chart-options="chartOptions" :chart-data="chartData" />
</template>

<script>
import { Bar } from 'vue-chartjs/legacy';
import { Chart as ChartJS, Tooltip, Legend, BarElement, CategoryScale, LinearScale } from 'chart.js';
import ChartDataLabels from 'chartjs-plugin-datalabels';

ChartJS.register(Tooltip, Legend, BarElement, CategoryScale, LinearScale, ChartDataLabels);

export default {
  components: { Bar },
  props: {
    height: {
      type: Number,
      default: 0,
    },
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
