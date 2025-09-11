<template>
  <v-container fluid class="d-flex justify-center pa-0 flex-column" fill-height>
    <h1 class="list_header">관리자 목록</h1>
    <v-card width="1360" class="mt-5 table_list_wrap table_list_wrap_admin">
      <v-card-text class="px-8 py-5 d-flex justify-space-between align-center table_list_bar">
        <div class="table_list_bar_text">
          등록된 관리자 계정
          <span>(총 {{ paging.totalElements }}명)</span>
        </div>
        <div class="d-flex align-center">
          <router-link to="/admin/register" class="register_admin_link">
            관리자 추가
            <v-icon class="ml-1">mdi-plus</v-icon>
          </router-link>
        </div>
      </v-card-text>
      <v-data-table
        :headers="adminListHeaders"
        :items="adminList"
        no-data-text="데이터가 존재하지 않습니다."
        :items-per-page="cellRange"
        :loading="tableLoading"
        fixed-header
        hide-default-footer
        class="table_wrap"
        page.sync="page">
        <template #item="row">
          <tr>
            <td>{{ row.item.num }}</td>
            <td>{{ row.item.adminName }}</td>
            <td>{{ row.item.adminPhoneNumber }}</td>
            <td>{{ row.item.adminLoginIdentifier }}</td>
            <td>{{ row.item.created }}</td>
            <td style="padding: 0">
              <button class="delete_btn" @click="showDeletePopup(row.item)">삭제</button>
              <button class="reset_btn ml-3" @click="showResetPopup(row.item)">비밀번호 초기화</button>
            </td>
          </tr>
        </template>
      </v-data-table>
      <v-pagination :value="paging.number" class="my-4" :length="paging.totalPages" @input="changePage({ query: $route.query, currentPage: paging.number, clickedPage: $event })"></v-pagination>
    </v-card>
    <popup v-model="showPopup" :popup="popup" @close="closePopup" @submit="submitPopup">
      <template #special>
        <delete-popup-form
          v-if="popup.special === 'user_delete'"
          v-model="deletePopupText"
          user-type="admin"
          :data="popup.data"
          :content="popup.content"
          @close="closePopup"
          @submit="submitPopup"></delete-popup-form>
      </template>
    </popup>
  </v-container>
</template>

<script>
import Popup from '@/components/common/Popup';
import DeletePopupForm from '@/components/common/DeletePopupForm';
import Api from '@/apis/api';
import { mapActions, mapGetters, mapState } from 'vuex';
import router from '@/router';
export default {
  name: 'List',
  components: {
    Popup,
    DeletePopupForm,
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
    },
    deletePopupText: '',
    paging: {
      number: 1,
      totalElements: 0,
      totalPages: 1,
    },
    cellRange: 10,
    adminList: [],
    tableLoading: true,
  }),
  computed: {
    ...mapGetters('table', ['getTableList']),
    ...mapState('commonValue', ['adminListHeaders']),
    ...mapState('user', ['adminIsSuper']),
  },
  watch: {
    adminIsSuper() {
      if (this.adminIsSuper) {
        // 새로고침 시 접근 가능 여부 확인 이후 목록 불러오기
        this.checkAccess();
        this.getList();
      }
    },
    '$route.query'() {
      this.getList();
    },
  },
  created() {
    if (this.adminIsSuper) {
      // 정상 경로로 접근 시 접근 여부 확인 이후 목록 불러오기
      this.checkAccess();
      this.getList();
    }
  },
  methods: {
    ...mapActions('table', ['changePage']),
    checkAccess() {
      if (this.adminIsSuper !== 'Y') {
        alert('잘못된 접근입니다.');
        router.push('/').catch(() => {});
        return false;
      }
    },
    closePopup() {
      this.deletePopupText = '';
      this.showPopup = false;
    },
    getList() {
      this.tableLoading = true;
      this.paging.number = this.$route.query.page ? parseInt(this.$route.query.page) : 1;
      Api.getAccountList({ page: this.paging.number, size: this.cellRange }).then(res => {
        if (res && res.data.rt === 200) {
          this.paging = res.data.response.paging;
          this.adminList = this.getTableList(res.data.response.paging, this.cellRange, res.data.response.adminList);
          this.tableLoading = false;
        } else {
          this.$root.alert.show({
            content: `알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요. 오류 코드 : ${res.data.rt}`,
          });
        }
      });
    },
    submitPopup(type, data) {
      if (type === 'user_delete') {
        this.deleteAdmin(data);
      } else if (type === 'reset') {
        this.resetUserPw(data);
      } else {
        this.closePopup();
      }
    },
    showDeletePopup(data) {
      this.popup = {
        type: 'info',
        header: '관리자 계정 삭제하기',
        btnType: '동의',
        special: 'user_delete',
        content: `계정을 삭제하면 관리자 목록에서
            <br />
            계정정보가 즉시 사라지고 복구가 불가합니다.
            <br />
            삭제 동의하시면 회원 이름을 한번 더 입력해주세요.`,
        data: data,
      };
      this.showPopup = true;
    },
    deleteAdmin(data) {
      Api.deleteAccount({ adminId: data.adminId }).then(res => {
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
    showResetPopup(data) {
      this.popup = {
        type: 'info',
        header: '비밀번호 초기화',
        content: `${data.adminName}님의 비밀번호를 초기화 하시겠습니까?`,
        btnType: '초기화',
        data: data,
      };
      this.showPopup = true;
    },
    resetUserPw(data) {
      Api.resetPassword({ adminId: data.adminId }).then(res => {
        if (res && res.data.rt === 200) {
          this.popup = {
            type: 'success',
            header: '비밀번호 초기화 완료',
            content: `비밀번호를 초기화 했습니다. <br/>
        초기임시 비밀번호는 <span style="color: #1251fc">${res.data.response.adminPassword}</span> 입니다.<br/><br/>※비밀번호 초기화 계정으로 로그인하면 자동으로 비밀번호 변경 페이지로 이동합니다.  (최초 1회)  반드시 비밀번호를 변경하고 시스템을 사용할 수 있게, 보안 주의사항을 안내해주세요.`,
            btnType: '확인',
            center: 'false',
          };
          this.showPopup = true;
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
