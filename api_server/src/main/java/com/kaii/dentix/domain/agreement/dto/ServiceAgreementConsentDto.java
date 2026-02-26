package com.kaii.dentix.domain.agreement.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kaii.dentix.domain.type.YnType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Date;

@Builder
public class ServiceAgreementConsentDto {

    /**
     * 사용자 약관 동의 수정 Request
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ModifyRequest {

        @NotNull(message = "동의 항목은 필수입니다.")
        private Long serviceAgreeId;

        @NotNull(message = "동의 여부는 필수입니다.")
        private YnType isUserServiceAgree;
    }

    /**
     * 사용자 약관 동의 수정 Response
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ModifyResponse {

        private Long serviceAgreeId;
        private YnType isUserServiceAgree;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private Date date;
    }

    /**
     * 사용자 약관 동의 조회 Item
     * - 목록/단건 둘 다 이 타입으로 내려도 됨
     * - JPA DTO 프로젝션 생성자 제공
     */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        private Long serviceAgreeId;
        private YnType isUserServiceAgree;
        private String serviceAgreeName;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private Date date;

        // JPA 프로젝션용(원래 UserServiceAgreementResponse에 있던 생성자 형태)
        public Response(Long serviceAgreeId, String serviceAgreeName, YnType isUserServiceAgree, Date date) {
            this.serviceAgreeId = serviceAgreeId;
            this.isUserServiceAgree = isUserServiceAgree;
            this.serviceAgreeName = serviceAgreeName;
            this.date = date;
        }

        // (원래 UserServiceAgreeList에 있던 String -> YnType 변환 생성자 필요하면 유지)
        public Response(Long serviceAgreeId, String serviceAgreeName, String isUserServiceAgree, Date date) {
            this.serviceAgreeId = serviceAgreeId;
            this.isUserServiceAgree = YnType.valueOf(isUserServiceAgree);
            this.serviceAgreeName = serviceAgreeName;
            this.date = date;
        }
    }
}