<template>
  <bar-chart :height="240" :chart-data="chartData" :chart-options="chartOptions"></bar-chart>
</template>

<script>
import BarChart from '@/components/chart/BarChart.vue';
import { mapState } from 'vuex';

export default {
  components: { BarChart },
  props: {
    chartData: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    chartOptions: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        datalabels: {
          display: false,
        },
        legend: {
          display: false,
        },
        tooltip: {
          caretSize: 0,
          displayColors: false,
          backgroundColor: 'rgba(3, 13, 33, 0.80)',
          bodyFont: {
            family: 'Pretendard',
            size: 18,
          },
          padding: 16,
        },
      },
      scales: {
        x: {
          grid: {
            drawBorder: false,
            display: false,
          },
          ticks: {
            color: '#2A2A2A',
            font: {
              family: 'Pretendard',
              size: 18,
              weight: 700,
            },
          },
        },
        y: {
          display: false,
        },
      },
    },
  }),
  computed: {
    ...mapState('commonValue', ['oralStatus']),
  },
  created() {
    this.chartOptions.scales.y.max = Math.max(...this.chartData.datasets[0].data); // 최댓값 설정
    const oralStatus = this.oralStatus;
    this.chartOptions.plugins.tooltip.callbacks = {
      title: function () {
        return '';
      },
      label: function (context) {
        return `${context.label}. ${oralStatus.find(s => s.value === context.label).text} ${context.parsed.y}회`;
      },
    };
  },
};
</script>
