<template>
  <div class="statistics_inner_wrap d-flex flex-wrap justify-center">
    <v-card width="1360" class="mt-15 pt-8 pb-5 search_list">
      <search-form @reset="reset" @search="search"></search-form>
    </v-card>
    <v-card class="chart_card subscription_rate_chart_wrap mt-5" width="1360">
      <div class="chart_header">
        <div class="d-flex align-center">
          <i class="icon status_list"></i>
          전체 남녀 가입률
        </div>
        <div>남녀 가입률 산출 기준은 앱 가입일입니다.</div>
      </div>
      <template v-if="userSignUpCountAll">
        <div class="chart_wrap d-flex">
          <div v-for="(item, index) in subscriptionRate" :key="index" class="chart_box" :class="{ margin: index !== 0 }">
            <div class="doughnut_chart_box">
              <status-doughnut-chart :description="item.description" :number="item.number" unit="명" :chart-data="item.chartData"></status-doughnut-chart>
            </div>
            <div>{{ item.chartLabel }}</div>
          </div>
        </div>
      </template>
      <no-data-box v-else></no-data-box>
    </v-card>
    <v-card class="chart_card oral_check_chart_wrap mt-5" width="1360">
      <div class="chart_header">
        <div class="d-flex align-center">
          <i class="icon status_list"></i>
          평균 구강촬영
        </div>
      </div>
      <template v-if="oralCheckRate && oralCheckStack">
        <div class="status_summary_wrap d-flex align-center">
          <i class="icon mr-3" :class="data.averageState ? `icon_${healthStatus[data.averageState].icon}` : `icon_norecord`"></i>
          <div class="status_text">
            <div>[덴티엑스] 내 평균 구강 상태는 {{ data.averageState ? healthStatus[data.averageState].text : '' }}입니다.</div>
            <span>전체 구강 촬영 횟수</span>
            <span>{{ `${data.oralCheckCount}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') }} 회 (환자당 평균 {{ `${data.oralCheckAverage}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') }}회 촬영)</span>
          </div>
        </div>
        <div class="chart_wrap d-flex">
          <div class="chart_box">
            <div class="horizontal_chart_box">
              <status-horizontal-bar-chart :chart-data="oralCheckRate.chartData"></status-horizontal-bar-chart>
            </div>
            <div>{{ oralCheckRate.chartLabel }}</div>
          </div>
          <div class="vertical-divide"></div>
          <div class="chart_box">
            <div class="main_chart_box">
              <status-stack-bar-chart :chart-data="oralCheckStack.chartData"></status-stack-bar-chart>
            </div>
          </div>
        </div>
      </template>
      <no-data-box v-else></no-data-box>
    </v-card>
    <v-card class="chart_card oral_status_chart_wrap mt-5" width="1360">
      <div class="chart_header">
        <div class="d-flex align-center">
          <i class="icon status_list"></i>
          평균 문진표 유형
        </div>
      </div>
      <template v-if="questionnaireResultTypeRate && questionnaireResultTypeList">
        <div class="status_summary_wrap d-flex align-center">
          <i class="icon mr-3" :class="mostQuestionnaireResultType ? `icon_${mostQuestionnaireResultType}` : `icon_norecord`"></i>
          <div class="status_text">
            <div>[덴티엑스] 내 가장 많은 유형은 {{ oralStatus.find(o => o.value === mostQuestionnaireResultType).text }}입니다.</div>
            <span>전체 검출 횟수</span>
            <span>
              {{ `${questionnaireResultTypeCountAll}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') }} 회 ({{ oralStatus.find(o => o.value === mostQuestionnaireResultType).text }}
              {{ `${data.allQuestionnaireResultTypeCount[`count${mostQuestionnaireResultType}`]}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') }}회)
            </span>
          </div>
        </div>
        <div class="chart_wrap d-flex">
          <div class="chart_box">
            <div class="horizontal_chart_box">
              <status-horizontal-bar-chart :chart-data="questionnaireResultTypeRate.chartData"></status-horizontal-bar-chart>
            </div>
            <div>{{ questionnaireResultTypeRate.chartLabel }}</div>
          </div>
          <div class="vertical-divide"></div>
          <div class="chart_box">
            <div class="main_chart_box">
              <status-bar-chart :chart-data="questionnaireResultTypeList.chartData"></status-bar-chart>
            </div>
          </div>
        </div>
      </template>
      <no-data-box v-else></no-data-box>
    </v-card>
  </div>
</template>

<script>
import SearchForm from '@/components/statistics/SearchForm';
import StatusDoughnutChart from '@/components/status/StatusDoughnutChart';
import StatusHorizontalBarChart from '@/components/status/StatusHorizontalBarChart.vue';
import StatusStackBarChart from '@/components/status/StatusStackBarChart.vue';
import StatusBarChart from '@/components/status/StatusBarChart.vue';
import NoDataBox from '@/components/common/NoDataBox';
import Api from '@/apis/api';
import { mapState } from 'vuex';

export default {
  name: 'PeriodStatistics',
  components: {
    SearchForm,
    StatusDoughnutChart,
    StatusHorizontalBarChart,
    StatusStackBarChart,
    StatusBarChart,
    NoDataBox,
  },
  data: () => ({
    data: {
      userSignUpCount: {},
      averageState: '',
      oralCheckCount: 0,
      oralCheckAverage: 0,
      oralCheckResultTypeCount: {},
      allQuestionnaireResultTypeCount: {},
    },
    tableLoading: false,
    subscriptionRate: [],
    userSignUpCountAll: 0,
    oralCheckRate: null,
    oralCheckStack: null,
    questionnaireResultTypeCountAll: 0,
    mostQuestionnaireResultType: null,
    questionnaireResultTypeRate: null,
    questionnaireResultTypeList: null,
  }),
  computed: {
    ...mapState('commonValue', ['healthStatus', 'oralStatus', 'statisticsRankListHeaders']),
  },
  watch: {
    '$route.query'() {
      this.getListStatisticsPeriod();
    },
  },
  created() {
    this.getListStatisticsPeriod();
  },
  methods: {
    getListStatisticsPeriod() {
      this.$root.spinner.set(true);
      this.tableLoading = true;
      let form = {};
      for (const i in this.$route.query) {
        if (i !== 'active') form[i] = this.$route.query[i];
      }
      Api.getStatisticsPeriod(form).then(res => {
        if (res && res.data.rt === 200) {
          for (const key in this.data) {
            this.data[key] = res.data.response[key];
          }
          this.setSubscriptionRate();
          this.setOralStatusData();
          this.setQuestionnaireResultTypeData();
          this.tableLoading = false;
          this.$root.spinner.set(false);
        } else {
          this.$root.alert.show({
            content: `알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요. 오류 코드 : ${res.data.rt}`,
          });
        }
      });
    },
    setSubscriptionRate() {
      this.userSignUpCountAll = this.data.userSignUpCount.countAll;
      this.subscriptionRate.push(
        {
          chartLabel: '전체 가입자 수',
          description: `100%`,
          number: this.data.userSignUpCount.countAll,
          chartData: {
            labels: ['남성', '여성', '미선택'],
            datasets: [
              {
                data: [
                  this.data.userSignUpCount.countMan,
                  this.data.userSignUpCount.countWoman,
                  this.data.userSignUpCount.countAll - this.data.userSignUpCount.countMan - this.data.userSignUpCount.countWoman,
                ],
                backgroundColor: ['#A9D0FC', '#FDC9C4', '#F0F0F0'],
                borderWidth: 0,
                cutout: '60%',
              },
            ],
          },
        },
        {
          chartLabel: '남성 가입자 수',
          description: `${Math.round((this.data.userSignUpCount.countMan / this.data.userSignUpCount.countAll) * 100)}%`,
          number: this.data.userSignUpCount.countMan,
          chartData: {
            labels: ['남성', '전체 - 남성'],
            datasets: [
              {
                data: [this.data.userSignUpCount.countMan, this.data.userSignUpCount.countAll - this.data.userSignUpCount.countMan],
                backgroundColor: ['#A9D0FC', '#F0F0F0'],
                borderWidth: 0,
                cutout: '60%',
              },
            ],
          },
        },
        {
          chartLabel: '여성 가입자 수',
          description: `${Math.round((this.data.userSignUpCount.countWoman / this.data.userSignUpCount.countAll) * 100)}%`,
          number: this.data.userSignUpCount.countWoman,
          chartData: {
            labels: ['여성', '전체 - 여성'],
            datasets: [
              {
                data: [this.data.userSignUpCount.countWoman, this.data.userSignUpCount.countAll - this.data.userSignUpCount.countWoman],
                backgroundColor: ['#FDC9C4', '#F0F0F0'],
                borderWidth: 0,
                cutout: '60%',
              },
            ],
          },
        },
      );
    },
    setOralStatusData() {
      let countAll = 0;
      let list = [];
      for (const key in this.data.oralCheckResultTypeCount) {
        const value = this.data.oralCheckResultTypeCount[key];
        countAll += value;
        const status = key.replace('count', '').toUpperCase();
        list.push({ status, value, color: this.healthStatus[status].color });
      }
      if (countAll) {
        // 상태 순서대로 정렬
        list.sort((a, b) => {
          return this.healthStatus[a.status].value < this.healthStatus[b.status].value ? 1 : -1;
        });
        this.oralCheckStack = {
          chartData: {
            labels: ['구강상태'],
            datasets: list.map(o => {
              return {
                label: this.healthStatus[o.status].text,
                barThickness: 80,
                data: [o.value],
                backgroundColor: [o.color],
                stack: 'stack',
              };
            }),
          },
        };
        // 개수 순서대로 정렬
        list.sort((a, b) => {
          return a.value <= b.value ? 1 : -1;
        });
        list = list.slice(0, 3);
        this.oralCheckRate = {
          chartLabel: '구강상태 순위',
          chartData: {
            labels: list.map((o, index) => `${index + 1}. ${this.healthStatus[o.status].text}`),
            datasets: [
              {
                total: countAll,
                data: list.map(o => o.value),
                backgroundColor: list.map(o => o.color),
              },
            ],
          },
        };
      }
    },
    setQuestionnaireResultTypeData() {
      let countAll = 0;
      let list = [];
      for (const key in this.data.allQuestionnaireResultTypeCount) {
        const value = this.data.allQuestionnaireResultTypeCount[key];
        countAll += value;
        const status = key.replace('count', '');
        list.push({ status, value, color: this.oralStatus.find(s => s.value === status).color });
      }
      if (countAll) {
        this.questionnaireResultTypeCountAll = countAll;
        // // 상태 순서대로 정렬
        list.sort();
        this.questionnaireResultTypeList = {
          chartData: {
            labels: list.map(o => o.status),
            datasets: [
              {
                barThickness: 40,
                data: list.map(o => o.value),
                backgroundColor: list.map(o => o.color),
              },
            ],
          },
        };
        // 개수 순서대로 정렬
        list.sort((a, b) => {
          return a.value <= b.value ? 1 : -1;
        });
        this.mostQuestionnaireResultType = list[0].status;
        list = list.slice(0, 3);
        this.questionnaireResultTypeRate = {
          chartLabel: '문진표 유형 순위',
          chartData: {
            labels: list.map(o => `${o.status}. ${this.oralStatus.find(s => s.value === o.status).text}`),
            datasets: [
              {
                total: countAll,
                data: list.map(o => o.value),
                backgroundColor: list.map(o => o.color),
              },
            ],
          },
        };
      }
    },
    reset() {
      if (JSON.stringify(this.$route.query) !== JSON.stringify({})) {
        this.$router.push({ query: null });
      }
    },
    search(data) {
      let query = {};
      for (const i in data) {
        if (data[i]) {
          query[i] = data[i];
        } else {
          delete query[i];
        }
      }
      if (JSON.stringify(query) !== JSON.stringify(this.$route.query)) {
        this.$router.push({ query: query });
      }
    },
  },
};
</script>

<style scoped></style>
