<template>
  <div class="organizationCopy-register">
    <h1>기관 등록</h1>
    <form @submit.prevent="registerOrganization">
      <label>기관명</label>
      <input v-model="org.name" type="text" required />

      <label>담당자</label>
      <input v-model="org.manager" type="text" required />

      <label>이메일</label>
      <input v-model="org.email" type="email" required />

      <label>전화번호</label>
      <input v-model="org.phone" type="text" required />

      <button type="submit">등록하기</button>
    </form>
  </div>
</template>

<script>
export default {
  name: "OrganizationRegisterPage",
  data() {
    return {
      org: {
        name: "",
        manager: "",
        email: "",
        phone: "",
      },
    };
  },
  methods: {
    registerOrganization() {
      console.log("기관 등록 데이터:", this.org);
      // TODO: API 연동 (src/apis/organizationCopy.js)
    },
  },
};
</script>

<style scoped>
.organizationCopy-register {
  padding: 20px;
}
.organizationCopy-register form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
