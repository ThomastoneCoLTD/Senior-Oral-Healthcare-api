package com.kaii.dentix.domain.admin.dto;

import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.dto.PageAndSizeRequest;
import com.kaii.dentix.global.common.dto.PagingDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

public class AdminDto {

    // =================================================================
    // 1. 관리자 목록 조회 요청 (Search)
    // =================================================================
    @Getter @Setter @SuperBuilder
    @AllArgsConstructor
    public static class SearchRequest extends PageAndSizeRequest {
        // 검색 조건이 있다면 여기에 추가 (예: keyword, role 등)
        // 현재는 페이징만 상속받음
    }

    // =================================================================
    // 2. 관리자 목록 응답 (List Response)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ListResponse {
        private PagingDTO paging;
        private List<Summary> adminList;

        public static ListResponse of(PagingDTO paging, List<Summary> adminList) {
            return ListResponse.builder()
                    .paging(paging)
                    .adminList(adminList)
                    .build();
        }
    }

    // 목록 내 단건 정보 (기존 AdminAccountDto 대체)
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Summary {
        private Long adminId;
        private String loginId;      // adminLoginIdentifier -> loginId
        private String name;         // adminName -> name
        private String phoneNumber;  // adminPhoneNumber -> phoneNumber
        private String createdDate;  // 가입일 (String or Date)

        // 필요하다면 슈퍼관리자 여부 등 추가
         private YnType isSuper;
    }
}