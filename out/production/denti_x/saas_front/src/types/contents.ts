// 콘텐츠 목록에서 쓰는 DTO
export interface ContentsDto {
  id: number;
  title: string;
  sort: number;
  type: "CARD" | "VIDEO" | "ANIMATION";
  typeColor: string;
  thumbnail: string;
  videoURL?: string;
  categoryIds: number[];
}

// 콘텐츠 상세 (카드뉴스/애니메이션)
export interface ContentsCardDto {
  number: number;
  path: string;
}

export interface ContentsCardListDto {
  title: string;
  cardList: ContentsCardDto[];
}

// API 공통 응답
export interface DataResponse<T> {
  rt: number;
  rtMsg: string;
  response: T;
}
