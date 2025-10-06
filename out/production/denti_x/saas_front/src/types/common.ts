// 공통 응답 타입

export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
}
export interface DataResponse<T> {
  data: T;
}

export interface DataResponse<T> {
  rt: number;
  rtMsg: string;
  response: T;
}