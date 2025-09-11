<template>
  <div class="doughnut_chart_box">
    <doughnut-chart :chart-data="chartData" :chart-options="chartOptions"></doughnut-chart>
    <div class="doughnut_inner_text">
      <div>{{ description }}</div>
      <div>
        <span>{{ number }}</span>
        {{ unit }}
      </div>
    </div>
  </div>
</template>

<script>
import DoughnutChart from '@/components/chart/DoughnutChart';

export default {
  name: 'StatusDoughnutChart',
  components: {
    DoughnutChart,
  },
  props: {
    description: {
      type: String,
      default: '',
    },
    number: {
      type: Number,
      default: 0,
    },
    unit: {
      type: String,
      default: '',
    },
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
        datalabels: {
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
          callbacks: {
            title: function () {
              return '';
            },
            label: function (context) {
              const dataList = context.dataset.data;
              const total = dataList.reduce((a, b) => a + b, 0); // data 총합
              const percent = total > 0 ? Math.round(((dataList[context.dataIndex] / total) * 100 + Number.EPSILON) * 10) / 10 : 0; // 소숫점 1자리까지 출력
              return `${context.label} : ${context.parsed}명 (${percent}%)`;
            },
          },
        },
      },
    },
  }),
};
</script>

<style scoped>
.doughnut_chart_box {
  position: relative;
}
.doughnut_inner_text {
  position: absolute;
  width: 150px;
  top: 51%;
  left: 51%;
  transform: translate(-50%, -50%);
  text-align: center;
  z-index: -1;
}
.doughnut_inner_text > div:nth-child(1) {
  font-size: 18px;
  color: #8a92a1;
}
.doughnut_inner_text > div:nth-child(2) {
  font-size: 18px;
  color: #656e81;
  line-height: normal;
}

.doughnut_inner_text > div:nth-child(2) > span {
  font-size: 42px;
  color: #030d21;
}
</style>
