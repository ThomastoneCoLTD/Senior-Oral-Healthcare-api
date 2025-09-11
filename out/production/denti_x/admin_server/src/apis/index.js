import axios from 'axios';
import store from '@/store/index';

const productionPath = 'https://dentix.kai-i.com/api'; // 실서버 API 경로
const developmentPath = 'https://dentix-dev.kai-i.com/api'; // 개발서버 API 경로
const accessTokenPath = '/login/access-token';

const baseURL = process.env.NODE_ENV === 'production' ? productionPath : developmentPath;
let apiBase = axios.create({
  baseURL: baseURL,
});

apiBase.defaults.headers.common['Content-Type'] = 'application/json';
apiBase.defaults.timeout = 60000; // 60초

apiBase.interceptors.response.use(
  async res => {
    if (res.data.rt === 403) {
      // token 재발행 및 반환
      const res2 = await issueAccessToken();
      // res.data : 서버 통신 성공, res.data.rt === 200 : 통신 이후 정상 토큰 발급 성공
      if (res2.data && res2.data.rt === 200) {
        res.config.headers.Authorization = store.state.user.accessToken;
      } else {
        return store.dispatch('user/logout');
      }
      return BaseApi().request(res.config);
    }
    return res;
  },
  () => {
    alert('서버를 불러오고 있습니다. 잠시후 시도해주세요.');
  },
);

// accessToken 재발급
export async function issueAccessToken() {
  try {
    const refreshToken = store.state.user.refreshToken ? store.state.user.refreshToken : localStorage.getItem('refreshToken');
    const res = await axios.put(baseURL + accessTokenPath, null, { headers: { RefreshToken: refreshToken } });
    store.commit('user/setAccessToken', res.data.response.accessToken);
    return res;
  } catch (err) {
    return err;
  }
}

let BaseApi = login => {
  const token = store.state.user.accessToken ? store.state.user.accessToken : localStorage.getItem('accessToken');

  if (login) {
    delete apiBase.defaults.headers.common['Authorization'];
  } else if (token) {
    apiBase.defaults.headers.common['Authorization'] = token;
  }

  return apiBase;
};

export default BaseApi;
