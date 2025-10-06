<template>
  <div class="usage-page">
    <h1>사용량 조회</h1>
    <div class="usage-summary">
      <p>API 호출: {{ usage.api }} / {{ quota.api }}</p>
      <progress :value="usage.api" :max="quota.api"></progress>

      <p>저장소 사용량: {{ usage.storage }}MB / {{ quota.storage }}MB</p>
      <progress :value="usage.storage" :max="quota.storage"></progress>

      <p>분석 횟수: {{ usage.analysis }} / {{ quota.analysis }}</p>
      <progress :value="usage.analysis" :max="quota.analysis"></progress>
    </div>
  </div>
</template>

<script>
export default {
  name: "UsagePage",
  data() {
    return {
      usage: { api: 120, storage: 500, analysis: 20 },
      quota: { api: 500, storage: 1000, analysis: 100 },
    };
  },
  created() {
    // TODO: API 호출로 실제 사용량 데이터 불러오기
  },
};
</script>

<style scoped>

.usage-page {
  padding: 20px;
}
.usage-summary progress {
  width: 100%;
  margin-bottom: 10px;
}
</style>
