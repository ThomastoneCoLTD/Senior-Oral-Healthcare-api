<template>
  <bar-chart :height="240" :chart-data="chartData" :chart-options="chartOptions" />
</template>

<script>
import BarChart from '@/components/chart/BarChart.vue';

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
          position: 'bottom',
          labels: {
            text: '123',
            boxWidth: 24,
            boxHeight: 24,
            color: '#2A2A2A',
            font: {
              family: 'Pretendard',
              size: 18,
              weight: 700,
            },
            padding: 24,
          },
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
      indexAxis: 'y',
      scales: {
        x: {
          display: false,
        },
        y: {
          display: false,
        },
      },
    },
  }),
  created() {
    const dataList = this.chartData.datasets.map(o => o.data[0]);
    const total = dataList.reduce((a, b) => a + b, 0); // data 총합
    this.chartOptions.scales.x.max = total; // 최댓값 설정
    this.chartOptions.plugins.tooltip.callbacks = {
      title: function () {
        return '';
      },
      label: function (context) {
        const percent = total > 0 ? Math.round(((dataList[context.datasetIndex] / total) * 100 + Number.EPSILON) * 10) / 10 : 0; // 소숫점 1자리까지 출력
        return `${context.dataset.label} : ${context.parsed.x}회 (${percent}%)`;
      },
    };
  },
};
</script>
