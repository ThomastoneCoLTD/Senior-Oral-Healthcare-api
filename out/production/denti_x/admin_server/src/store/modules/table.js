import router from '@/router';

export default {
  namespaced: true,
  actions: {
    // 페이징 처리
    // eslint-disable-next-line no-empty-pattern
    changePage({}, payload) {
      if (payload.currentPage === payload.clickedPage) return;
      router.push({ query: Object.assign({}, payload.query, { page: payload.clickedPage }) });
    },
  },
  getters: {
    // 테이블 row 번호 추가하여 반환
    getTableList() {
      return (paging, size, list) => {
        const maxNum = paging.totalElements - size * (paging.number - 1);
        return list.map((row, index) => {
          row.num = maxNum - index;
          return row;
        });
      };
    },
  },
};
