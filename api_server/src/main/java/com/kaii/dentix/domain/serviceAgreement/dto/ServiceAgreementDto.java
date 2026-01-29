package com.kaii.dentix.domain.serviceAgreement.dto;

import com.kaii.dentix.domain.serviceAgreement.domain.ServiceAgreement;
import com.kaii.dentix.domain.type.YnType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter @Builder
@AllArgsConstructor
public class ServiceAgreementDto {

    // =================================================================
    // 1. 약관 목록 응답 (ListResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> serviceAgreement;
    }

    // =================================================================
    // 2. 단일 약관 정보 (Response)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String menuName;
        private YnType isServiceAgreeRequired;
        private String path;

        // Entity -> DTO 변환 편의 메서드
        public static Response from(ServiceAgreement entity) {
            return Response.builder()
                    .id(entity.getServiceAgreeId())
                    .name(entity.getServiceAgreeName())
                    .menuName(entity.getServiceAgreeMenuName())
                    .isServiceAgreeRequired(entity.getIsServiceAgreeRequired())
                    .path(entity.getServiceAgreePath())
                    .build();
        }
    }
}
