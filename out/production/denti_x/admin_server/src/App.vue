<template>
  <div>
    <router-view :key="$route.fullPath" />
    <!-- toast -->
    <toast ref="toast" />
    <spinner ref="spinner" />
    <alert ref="alert" />
  </div>
</template>

<script>
import Toast from '@/components/common/Toast';
import Spinner from '@/components/common/Spinner';
import Alert from '@/components/common/Alert';
export default {
  name: 'App',
  components: {
    Toast,
    Spinner,
    Alert,
  },
  mounted() {
    this.$root.toast = this.$refs.toast;
    this.$root.spinner = this.$refs.spinner;
    this.$root.alert = this.$refs.alert;
  },
};
</script>
