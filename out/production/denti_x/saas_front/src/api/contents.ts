// src/api/contents.ts
import api from "./api";  // ✅ 방금 만든 api.ts 불러옴

export const getContents = () => api.get("/contents");
export const getContentDetail = (id: string) => api.get(`/contents/card?contentsId=${id}`);
