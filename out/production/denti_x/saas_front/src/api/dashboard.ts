import api from "./api";
import type { DataResponse, DashboardDto } from "../types/dashboard";

export const getDashboardData = async () => {
  const res = await api.get<DataResponse<DashboardDto>>("/oral-check/dashboard");
  return res.data; // ✅ 이제 response 안에 DashboardDto 있음
};
