<template>
  <div class="subscription-page">
    <h1>구독 플랜</h1>
    <div class="plans">
      <div v-for="plan in plans" :key="plan.name" class="plan-card">
        <h2>{{ plan.name }}</h2>
        <p>{{ plan.price }}</p>
        <ul>
          <li v-for="f in plan.features" :key="f">✅ {{ f }}</li>
        </ul>
        <button @click="subscribe(plan)">구독하기</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "SubscriptionPage",
  data() {
    return {
      plans: [
        { name: "Free", price: "₩0", features: ["기본 기능 제공"] },
        { name: "Basic", price: "₩9,900/월", features: ["추가 기능", "고객 지원"] },
        { name: "Premium", price: "₩29,900/월", features: ["모든 기능 무제한", "프리미엄 지원"] },
      ],
    };
  },
  methods: {
    subscribe(plan) {
      console.log("선택한 플랜:", plan);
      // TODO: API 연동 (src/apis/subscription.js)
    },
  },
};
</script>

<style scoped>
.subscription-page {
  padding: 20px;
}
.plans {
  display: flex;
  gap: 20px;
}
.plan-card {
  border: 1px solid #ccc;
  padding: 16px;
  border-radius: 8px;
  width: 250px;
}
</style>
