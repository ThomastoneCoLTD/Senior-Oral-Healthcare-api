// 로그인/회원가입 관련 타입

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  userId?: number;
  userName?: string; // ✅ 백엔드 응답에 맞춤
  role?: string;
}

export interface LoginRequest {
  userType: "user" | "admin";
  loginId: string;
  password: string;
}
