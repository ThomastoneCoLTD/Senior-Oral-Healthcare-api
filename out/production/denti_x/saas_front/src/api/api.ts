import axios from "axios";

// ✅ axios 인스턴스 생성
const api = axios.create({
  baseURL: "http://localhost:8080", // 또는 "http://localhost:8080"
  headers: {
    "Content-Type": "application/json",
  },
});

// ✅ 요청 시 토큰 자동 첨부 (선택)
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
