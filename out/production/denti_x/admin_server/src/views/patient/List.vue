<template>
  <v-container fluid class="d-flex justify-center pa-0 flex-wrap container__wrap">
    <v-card width="1360" class="mt-15 pt-8 pb-5 search_list">
      <search-form @reset="reset" @search="search"></search-form>
    </v-card>
    <v-card width="1360" class="mt-5 mb-12 table_list_wrap table_list_wrap_patient">
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
        :headers="patientListHeaders"
        :items="patientList"
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
            <td>{{ row.item.patientName }}</td>
            <td>{{ row.item.patientPhoneNumber }}</td>
            <td>{{ row.item.created ? row.item.created.replaceAll('-', '.') : '-' }}</td>
            <td>{{ row.item.isUser === 'Y' ? '가입' : '-' }}</td>
            <td v-if="row.item.isUser !== 'Y'" style="padding: 0">
              <button class="delete_btn" @click="showDeletePopup(row.item)">삭제하기</button>
            </td>
            <td v-else>-</td>
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
        <delete-popup-form
          v-if="popup.special === 'user_delete'"
          v-model="deletePopupText"
          user-type="patient"
          :data="popup.data"
          :content="popup.content"
          @close="closePopup"
          @submit="submitPopup"></delete-popup-form>
      </template>
    </popup>
  </v-container>
</template>

<script>
import Api from '@/apis/api';
import Popup from '@/components/common/Popup';
import DeletePopupForm from '@/components/common/DeletePopupForm';
import SelectBox from '@/components/common/SelectBox';
import SearchForm from '@/components/patient/SearchForm';
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
    paging: {
      number: 1,
      totalElements: 0,
      totalPages: 1,
    },
    cellRange: 50,
    patientList: [],
    tableLoading: true,
    todayDate: '',
  }),
  computed: {
    ...mapGetters('table', ['getTableList']),
    ...mapState('commonValue', ['cellRangeItems', 'patientListHeaders']),
  },
  watch: {
    '$route.query'() {
      this.getList();
    },
  },
  created() {
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
        form[i] = this.$route.query[i];
      }
      Api.getPatientList(form).then(res => {
        if (res && res.data.rt === 200) {
          this.paging = res.data.response.paging;
          this.patientList = this.getTableList(this.paging, this.cellRange, res.data.response.patientList);
          this.tableLoading = false;
        } else {
          this.$root.alert.show({
            content: `알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요. 오류 코드 : ${res.data.rt}`,
          });
        }
      });
    },
    customSort(items, index, isDesc) {
      if (!index[0]) return items; // 정렬 없는 경우 그대로 리턴

      items.sort((a, b) => {
        if (!a[index[0]] || !b[index[0]]) return a[index[0]] ? -1 : 1; // 값이 없는게 있으면 맨 아래로 정렬

        const sort = a[index[0]] < b[index[0]] ? -1 : 1;

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
        this.deletePatient(this.popup.data);
      }
      this.closePopup();
    },
    showDeletePopup(item) {
      this.popup = {
        type: 'info',
        header: '환자 삭제하기',
        btnType: '동의',
        special: 'user_delete',
        content: `계정을 삭제하면 환자 목록에서
            <br />
            환자 정보가 즉시 사라지고 복구가 불가합니다.
            <br /><br />
            삭제 동의하시면 환자 이름을 한번 더 입력해주세요.`,
        data: item,
      };
      this.showPopup = true;
    },
    deletePatient(data) {
      Api.deletePatient(data).then(res => {
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
  },
};
</script>

<style scoped></style>
