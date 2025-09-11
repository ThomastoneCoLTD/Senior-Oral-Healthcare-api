<template>
  <div class="status_img_box">
    <img :src="require(`@/assets/img/report/${parent === 'statistics' ? 'grey_empty_teeth' : 'empty_teeth'}.png`)" alt="치아 상태 상세" width="240" />
    <template v-if="Object.keys(data).length !== 0">
      <template v-if="data.upRight">
        <img :src="require(`@/assets/img/report/tr_${healthStatus[data.upRight.state].icon}.png`)" alt="오른쪽 위 치아 상태" width="240" />
        <div class="percentage_box up_right">
          {{ labelText ? healthStatus[data.upRight.state].text : `${data.upRight.percent}%` }}
          <img src="@/assets/img/report/tr_line.png" alt="line" />
        </div>
      </template>

      <template v-if="data.upLeft">
        <img :src="require(`@/assets/img/report/tl_${healthStatus[data.upLeft.state].icon}.png`)" alt="왼쪽 위 치아 상태" width="240" />
        <div class="percentage_box up_left">
          {{ labelText ? healthStatus[data.upLeft.state].text : `${data.upLeft.percent}%` }}
          <img src="@/assets/img/report/tl_line.png" alt="line" />
        </div>
      </template>

      <template v-if="data.downRight">
        <img :src="require(`@/assets/img/report/br_${healthStatus[data.downRight.state].icon}.png`)" alt="오른쪽 아래 치아 상태" width="240" />
        <div class="percentage_box down_right">
          <img src="@/assets/img/report/br_line.png" alt="line" />
          {{ labelText ? healthStatus[data.downRight.state].text : `${data.downRight.percent}%` }}
        </div>
      </template>

      <template v-if="data.downLeft">
        <img :src="require(`@/assets/img/report/bl_${healthStatus[data.downLeft.state].icon}.png`)" alt="왼쪽 아래 치아 상태" width="240" />
        <div class="percentage_box down_left">
          <img src="@/assets/img/report/bl_line.png" alt="line" />
          {{ labelText ? healthStatus[data.downLeft.state].text : `${data.downLeft.percent}%` }}
        </div>
      </template>
    </template>
  </div>
</template>

<script>
import { mapState } from 'vuex';

export default {
  name: 'TeethBox',
  props: {
    data: {
      type: Object,
      default: () => {
        return {
          upRight: {},
          downRight: {},
          upLeft: {},
          downLeft: {},
        };
      },
    },
    parent: {
      type: String,
      default: '',
    },
    labelText: Boolean,
  },
  computed: {
    ...mapState('commonValue', ['healthStatus']),
  },
};
</script>

<style scoped>
.status_img_box {
  width: 240px;
  height: 180px;
  position: relative;
}
.status_img_box > img:nth-child(n + 1) {
  position: absolute;
}
.status_img_box > img:nth-child(2) {
  top: 0;
  left: 0;
}
.status_img_box > img:nth-child(3) {
  top: 0;
  right: 0;
}
.status_img_box > img:nth-child(4) {
  bottom: 0;
  left: 0;
}
.status_img_box > img:nth-child(5) {
  bottom: 0;
  right: 0;
}
.percentage_box {
  position: absolute;
  font-weight: 700;
}
.up_right {
  top: -20px;
  left: 30px;
}
.up_right img {
  display: block;
  margin-left: 10px;
  margin-top: -5px;
}
.up_left {
  top: -20px;
  right: 30px;
  text-align: right;
}
.up_left img {
  display: block;
  margin-right: 10px;
  margin-top: -5px;
}
.down_right {
  bottom: -20px;
  left: 30px;
}
.down_right img {
  display: block;
  margin-left: 10px;
  margin-bottom: -5px;
}
.down_left {
  bottom: -20px;
  right: 30px;
  text-align: right;
}
.down_left img {
  display: block;
  margin-right: 10px;
  margin-bottom: -5px;
}
</style>
