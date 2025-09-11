import BaseApi from './index';

export default {
  // 관리자 로그인
  login(form) {
    return BaseApi(true).post('/admin/login', form);
  },
  // 관리자 로그인 정보 업데이트
  autoLogin() {
    return BaseApi().put('/admin/account/auto-login');
  },
  // 관리자 계정 목록 조회
  getAccountList(form) {
    return BaseApi().get('/admin/account/list', { params: form });
  },
  // 관리자 비밀번호 변경
  passwordModify(form) {
    return BaseApi().put('/admin/account/password', form);
  },
  // 관리자 계정 생성
  createAccount(form) {
    return BaseApi().post('/admin/account', form);
  },
  // 관리자 비밀번호 초기화
  resetPassword(form) {
    return BaseApi().put(`/admin/account/reset-password?adminId=${form.adminId}`);
  },
  // 관리자 계정 삭제
  deleteAccount(form) {
    return BaseApi().delete(`/admin/account?adminId=${form.adminId}`);
  },
  // 사용자 목록 조회
  getUserList(form) {
    return BaseApi().get('/admin/user', { params: form });
  },
  // 사용자 인증
  verifyUser(form) {
    return BaseApi().put(`/admin/user/verify?userId=${form.userId}`);
  },
  // 사용자 정보 조회
  getUser(form) {
    return BaseApi().get('/admin/user/info', { params: form });
  },
  // 사용자 정보 수정
  modifyUser(form) {
    return BaseApi().put('/admin/user', form);
  },
  // 사용자 삭제
  deleteUser(form) {
    return BaseApi().delete(`/admin/user?userId=${form.userId}`);
  },
  // 기간별 통계
  getStatisticsPeriod(form) {
    return BaseApi().get('/admin/statistic', { params: form });
  },
  // 환자 등록
  createPatient(form) {
    return BaseApi().post('/admin/patient', form);
  },
  // 환자 목록
  getPatientList(form) {
    return BaseApi().get('/admin/patient', { params: form });
  },
  // 환자 삭제
  deletePatient(form) {
    return BaseApi().delete(`/admin/patient?patientId=${form.patientId}`);
  },
};
