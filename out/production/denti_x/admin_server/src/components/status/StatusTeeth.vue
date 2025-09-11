<template>
  <div class="teeth_status_wrap">
    <div class="d-flex">
      <div v-for="(item, index) in data.slice(0, 3)" :key="index" class="teeth_status_box" :class="{ margin: index !== 0 }">
        <div class="status_text_wrap">
          <div class="d-flex align-center">
            <i class="icon mr-3" :class="item ? `icon_${healthStatus[item.total.state].icon}` : `icon_norecord`"></i>
            <div class="status_text">
              <div>{{ index + 1 }}차 {{ item ? `구강상태는 ${healthStatus[item.total.state].paragraph}` : '촬영경과가 없습니다.' }}</div>
              <span>양치시간</span>
              <span>{{ item ? $convertDateFormat('AMPMKR', item.date) : '미촬영' }}</span>
            </div>
          </div>
          <div v-html="item ? healthStatus[item.total.state].infoText : emptyDataText"></div>
        </div>
        <div class="status_img_wrap d-flex justify-space-between align-center">
          <span>우</span>
          <teeth-box :data="item ? item : {}"></teeth-box>
          <span>좌</span>
        </div>
      </div>
    </div>
    <div class="intro_box mt-5 d-flex align-end">
      <ul>
        <li class="intro_text_up ml-2">
          <i class="color_box blue"></i>
          4개
        </li>
        <li class="bar"></li>
        <li class="intro_text_down">
          <i class="icon tooth"></i>
          건강: 0%
        </li>
      </ul>
      <ul>
        <li class="intro_text_up ml-2">
          <i class="color_box green"></i>
          1개 이상
        </li>
        <li class="bar"></li>
        <li class="intro_text_down">
          <i class="icon tooth"></i>
          양호: 1~9%
        </li>
      </ul>
      <ul>
        <li class="intro_text_up ml-2">
          <i class="color_box yellow"></i>
          1개 이상
        </li>
        <li class="bar"></li>
        <li class="intro_text_down">
          <i class="icon tooth"></i>
          주의: 10~29%
        </li>
      </ul>
      <ul>
        <li class="intro_text_up mb-2 ml-14">
          <i class="color_box yellow"></i>
          3개 이상
        </li>
        <li class="intro_text_up ml-14">
          <i class="color_box red"></i>
          1개 이상
        </li>
        <li class="bar"></li>
        <li class="intro_text_down ml-12">
          <i class="icon tooth"></i>
          위험: 30% 이상
        </li>
      </ul>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';
import TeethBox from '@/components/common/TeethBox';
export default {
  name: 'StatusTeeth',
  components: {
    TeethBox,
  },
  props: {
    data: {
      type: Array,
      default: () => [],
    },
  },
  data: () => ({
    emptyDataText: '하루에 아침, 점심, 저녁 세 번 이상 양치 후<br/>구강촬영 할 수 있도록 격려해주세요.',
  }),
  computed: {
    ...mapState('commonValue', ['healthStatus']),
  },
};
</script>

<style scoped></style>
