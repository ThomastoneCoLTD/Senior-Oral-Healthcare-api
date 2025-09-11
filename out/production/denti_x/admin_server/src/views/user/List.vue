<template>
  <v-container fluid class="d-flex justify-center pa-0 flex-wrap container__wrap">
    <v-card width="1360" class="mt-15 pt-8 pb-5 search_list">
      <search-form @reset="reset" @search="search"></search-form>
    </v-card>
    <v-card width="1360" class="mt-5 mb-12 table_list_wrap table_list_wrap_user">
      <v-card-text class="px-8 py-5 d-flex justify-space-between align-center table_list_bar">
        <div class="table_list_bar_text">
          검색 결과
          <span>(총 {{ paging.totalElements }}명)</span>
        </div>
        <div class="d-flex align-center">
          <div class="select_wrap">
            <select-box v-model="cellRange" :items="cellRangeItems" height="36" @fieldChanged="changeCellRange"></select-box>
          </div>
        </div>
      </v-card-text>
      <v-data-table
        :headers="userListHeaders"
        :items="userList"
        :items-per-page="cellRange"
        :custom-sort="customSort"
        fixed-header
        hide-default-footer
        no-data-text="데이터가 존재하지 않습니다."
        class="table_wrap"
        page.sync="page"
        :loading="tableLoading">
        <template #item="row">
          <tr>
            <td>{{ row.item.num }}</td>
            <td>{{ row.item.userLoginIdentifier }}</td>
            <td>{{ row.item.userName }}</td>
            <td>{{ row.item.userGender ? genderStatus.find(o => o.value === row.item.userGender).text : '-' }}</td>
            <td>{{ row.item.patientPhoneNumber || '-' }}</td>
            <td v-html="row.item.oralStatus ? getOralStatusText(row.item.oralStatus) : '-'"></td>
            <td>{{ row.item.questionnaireDate ? row.item.questionnaireDate.replaceAll('-', '.') : '-' }}</td>
            <td>{{ row.item.oralCheckResultTotalType ? healthStatus[row.item.oralCheckResultTotalType].text : '-' }}</td>
            <td>{{ row.item.oralCheckDate ? row.item.oralCheckDate.replaceAll('-', '.') : '-' }}</td>
            <td>{{ row.item.isVerify === 'Y' ? '인증됨' : '-' }}</td>
            <td v-if="row.item.isVerify !== 'Y'" style="padding: 0">
              <button class="delete_btn" @click="showVerifyPopup(row.item)">회원인증</button>
            </td>
            <td v-else>-</td>
            <td style="padding: 0">
              <button class="delete_btn" @click="$router.push(`/user/modify/${row.item.userId}`)">수정하기</button>
            </td>
            <td style="padding: 0">
              <button class="delete_btn" @click="showDeletePopup(row.item)">삭제하기</button>
            </td>
          </tr>
        </template>
      </v-data-table>
      <v-pagination
        :value="paging.number"
        total-visible="10"
        class="my-4"
        :length="paging.totalPages"
        @input="changePage({ query: $route.query, currentPage: paging.number, clickedPage: $event })"></v-pagination>
    </v-card>
    <popup v-model="showPopup" :popup="popup" @close="closePopup" @submit="submitPopup">
      <template #special>
        <delete-popup-form v-if="popup.special === 'user_delete'" v-model="deletePopupText" :data="popup.data" :content="popup.content" @close="closePopup" @submit="submitPopup"></delete-popup-form>
      </template>
    </popup>
  </v-container>
</template>

<script>
import Api from '@/apis/api';
import Popup from '@/components/common/Popup';
import DeletePopupForm from '@/components/common/DeletePopupForm';
import SelectBox from '@/components/common/SelectBox';
import SearchForm from '@/components/user/SearchForm';
import { mapActions, mapGetters, mapState } from 'vuex';
export default {
  name: 'List',
  components: {
    Popup,
    SearchForm,
    DeletePopupForm,
    SelectBox,
  },
  data: () => ({
    showPopup: false,
    popup: {
      type: '',
      header: '',
      content: '',
      btnType: '',
      center: '',
      special: '',
      data: '',
    },
    deletePopupText: '',
    active: {},
    isPhotographed: false,
    paging: {
      number: 1,
      totalElements: 0,
      totalPages: 1,
    },
    cellRange: 50,
    userList: [],
    tableLoading: true,
    todayDate: '',
  }),
  computed: {
    ...mapGetters('table', ['getTableList']),
    ...mapState('commonValue', ['oralStatus', 'healthStatus', 'genderStatus', 'verifyStatus', 'cellRangeItems', 'userListHeaders']),
  },
  watch: {
    '$route.query'() {
      this.getList();
    },
  },
  created() {
    this.setTodayDate();
    this.getList();
  },
  methods: {
    ...mapActions('table', ['changePage']),
    getList() {
      this.tableLoading = true;
      this.paging.number = this.$route.query.page ? parseInt(this.$route.query.page) : 1;
      this.cellRange = this.$route.query.size ? parseInt(this.$route.query.size) : 50;
      let form = {
        page: this.paging.number,
        size: this.cellRange,
      };
      for (const i in this.$route.query) {
        this.isPhotographed = this.$route.query['isPhotographed'] === 'Y';
        if (i !== 'active') form[i] = this.$route.query[i];
      }
      Api.getUserList(form).then(res => {
        if (res && res.data.rt === 200) {
          this.paging = res.data.response.paging;
          this.userList = this.getTableList(this.paging, this.cellRange, res.data.response.userList);
          this.tableLoading = false;
        } else {
          this.$root.alert.show({
            content: `알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요. 오류 코드 : ${res.data.rt}`,
          });
        }
      });
    },
    getOralStatusText(oralStatus) {
      return oralStatus
        .split(',')
        .map(s => this.oralStatus.find(o => o.value === s).text)
        .join('<br/>');
    },
    customSort(items, index, isDesc) {
      if (!index[0]) return items; // 정렬 없는 경우 그대로 리턴

      items.sort((a, b) => {
        if (!a[index[0]] || !b[index[0]]) return a[index[0]] ? -1 : 1; // 값이 없는게 있으면 맨 아래로 정렬

        let sort;
        if (index[0] === 'oralCheckResultTotalType') {
          // 구강상태
          sort = this.healthStatus[b.oralCheckResultTotalType].value - this.healthStatus[a.oralCheckResultTotalType].value;
        } else if (index[0] === 'oralStatus') {
          sort = this.getOralStatusText(a[index[0]]) < this.getOralStatusText(b[index[0]]) ? -1 : 1;
        } else {
          sort = a[index[0]] < b[index[0]] ? -1 : 1;
        }

        return !isDesc[0] ? sort : -sort;
      });

      return items;
    },
    reset() {
      if (JSON.stringify(this.$route.query) !== JSON.stringify({})) {
        this.$router.push({ query: null });
      }
    },
    search(form) {
      for (const i in form) {
        if (!form[i]) {
          delete form[i]; // query에 빈 값으로 key만 출력되는 현상 방지
        }
      }
      if (JSON.stringify(form) !== JSON.stringify(this.$route.query)) {
        this.$router.push({ query: form });
      }
    },
    changeCellRange(value) {
      this.$router.push({ query: { size: value } });
    },
    closePopup() {
      this.deletePopupText = '';
      this.showPopup = false;
    },
    submitPopup(type) {
      if (type === 'user_delete') {
        this.deleteUser(this.popup.data);
      } else if (type === 'verify') {
        this.verifyUser(this.popup.data);
      }
      this.closePopup();
    },
    showDeletePopup(item) {
      this.popup = {
        type: 'info',
        header: '회원 계정 삭제하기',
        btnType: '동의',
        special: 'user_delete',
        content: `계정을 삭제하면 회원 목록에서
            <br />
            계정정보가 즉시 사라지고 복구가 불가합니다.
            <br/>
            <br />
            또 앱 서비스 이용시 즉시 로그아웃됩니다.
           <br/>
            삭제 동의하시면 회원 이름을 한번 더 입력해주세요.`,
        data: item,
      };
      this.showPopup = true;
    },
    showVerifyPopup(data) {
      this.popup = {
        type: 'info',
        header: '회원 인증하기',
        content: `${data.userName}님을 인증하시겠습니까?`,
        btnType: '인증하기',
        data: data,
      };
      this.showPopup = true;
    },
    verifyUser(data) {
      Api.verifyUser({ userId: data.userId }).then(res => {
        if (res && res.data.rt === 200) {
          this.closePopup();
          this.$root.toast.show({
            content: `인증되었습니다.`,
          });
          this.getList();
        } else {
          this.$root.alert.show({
            content: `알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요. 오류 코드 : ${res.data.rt}`,
          });
        }
      });
    },
    deleteUser(data) {
      Api.deleteUser(data).then(res => {
        if (res && res.data.rt === 200) {
          this.closePopup();
          this.$root.toast.show({
            content: `삭제가 완료되었습니다.`,
          });
          this.getList();
        } else {
          this.$root.alert.show({
            content: `알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요. 오류 코드 : ${res.data.rt}`,
          });
        }
      });
    },
    setTodayDate() {
      const today = new Date();
      const year = today.getFullYear();
      const month = today.getMonth() + 1 < 10 ? `0${today.getMonth() + 1}` : today.getMonth() + 1;
      const date = today.getDate() < 10 ? `0${today.getDate()}` : today.getDate();
      this.todayDate = `${year}-${month}-${date}`;
    },
  },
};
</script>

<style scoped></style>
