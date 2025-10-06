import api from "./client";

export interface LoginRequest {
  userType: "admin" | "user";
  loginId: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  userId?: number;
  username?: string;
  role?: string;
}

export const loginApi = (data: LoginRequest) =>
  api.post<LoginResponse>("/login", data);
