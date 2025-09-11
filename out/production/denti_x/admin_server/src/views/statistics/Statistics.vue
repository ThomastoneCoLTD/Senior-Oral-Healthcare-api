<template>
  <v-container fluid class="d-flex justify-center pa-0 flex-wrap statistics_wrap">
    <!--<div class="tab_wrap d-flex justify-center">-->
    <!--  <div class="d-flex">-->
    <!--    <router-link v-for="(tab, index) in tabs" :key="index" active-class="active" :to="tab.route" class="tab_item d-flex align-center justify-center" :class="{ 'ml-1': index !== 0 }">-->
    <!--      {{ tab.name }}-->
    <!--    </router-link>-->
    <!--  </div>-->
    <!--</div>-->
    <router-view :key="$route.fullPath" class="container__wrap"></router-view>
  </v-container>
</template>

<script>
export default {
  name: 'Statistics',
  data: () => ({
    tabs: [
      { name: '기간별 통계', route: `/statistics/period` },
      { name: '치아별 구강상태', route: `/statistics/oral` },
    ],
  }),
};
</script>

<style scoped></style>
