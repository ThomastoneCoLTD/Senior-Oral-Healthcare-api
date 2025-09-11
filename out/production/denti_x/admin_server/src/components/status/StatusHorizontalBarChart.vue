<template>
  <bar-chart :chart-data="chartData" :chart-options="chartOptions" />
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
      plugins: {
        legend: {
          display: false, // 범례를 비활성화합니다.
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
        datalabels: {
          formatter: function (value, context) {
            var idx = context.dataIndex; // 각 데이터 인덱스

            // 출력 텍스트
            return context.chart.data.labels[idx];
          },
          anchor: 'start',
          align: 'right',
          color: '#2A2A2A',
          font: {
            size: 20,
            weight: 'bold',
            lineHeight: 20,
            family: 'Pretendard',
          },
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
    this.chartOptions.scales.x.max = this.chartData.datasets[0].data[0]; // 최댓값 설정
    this.chartOptions.plugins.tooltip.callbacks = {
      title: function () {
        return '';
      },
      label: function (context) {
        const dataList = context.dataset.data;
        const total = context.dataset.total;
        const percent = total > 0 ? Math.round(((dataList[context.dataIndex] / total) * 100 + Number.EPSILON) * 10) / 10 : 0; // 소숫점 1자리까지 출력
        return `${context.label} : ${context.parsed.x}회 (${percent}%)`;
      },
    };
  },
};
</script>
