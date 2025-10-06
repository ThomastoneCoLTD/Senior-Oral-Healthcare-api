import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080",
});

// 로그인 API
export const login = (
  userType: "admin" | "user",
  loginId: string,
  password: string
) => api.post("/login", { userType, loginId, password });

// 요청마다 토큰 자동 추가
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
