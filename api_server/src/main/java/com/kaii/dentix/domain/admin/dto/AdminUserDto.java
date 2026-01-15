package com.kaii.dentix.domain.admin.dto;

import com.kaii.dentix.domain.type.DatePeriodType;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import com.kaii.dentix.global.common.dto.PageAndSizeRequest;
import com.kaii.dentix.global.common.dto.PagingDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AdminUserDto {

    // =================================================================
    // 1. 사용자 목록 조회 (Search / List)
    // =================================================================
    @Getter @Setter @SuperBuilder
    @NoArgsConstructor @AllArgsConstructor
    public static class SearchRequest extends PageAndSizeRequest {
        private Long organizationId;
        private String keyword;       // userIdentifierOrName -> keyword (단축)
        private OralCheckResultType oralCheckResultTotalType;
        private String oralStatus;
        private GenderType gender;    // userGender -> gender
        private YnType isVerify;

        // 날짜 검색
        private DatePeriodType datePeriodType; // allDatePeriod -> datePeriodType
        private String startDate;
        private String endDate;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ListResponse {
        private PagingDTO paging;
        private List<Info> userList;

        public static ListResponse of(PagingDTO paging, List<Info> userList) {
            return ListResponse.builder()
                    .paging(paging)
                    .userList(userList)
                    .build();
        }
    }

    // 목록 내 단건 정보 (기존 AdminUserInfoDto)
    @Getter @Setter
    @NoArgsConstructor
    public static class Info {
        private Long userId;
        private String loginId;     // userLoginIdentifier
        private String name;        // userName
        private GenderType gender;  // userGender
        private String oralStatusTitle;
        private Date questionnaireDate;
        private OralCheckResultType oralCheckResultTotalType;
        private Date oralCheckDate;
        private YnType isVerify;
        private List<String> serviceNames;

        //QueryDSL Projections.constructor 용 생성자
        public Info(
                Long userId, String loginId, String name, GenderType gender,
                String oralStatusTitle, Date questionnaireDate,
                OralCheckResultType oralCheckResultTotalType, Date oralCheckDate,
                YnType isVerify, String serviceNamesStr
        ) {
            this.userId = userId;
            this.loginId = loginId;
            this.name = name;
            this.gender = gender;
            this.oralStatusTitle = oralStatusTitle;
            this.questionnaireDate = questionnaireDate;
            this.oralCheckResultTotalType = oralCheckResultTotalType;
            this.oralCheckDate = oralCheckDate;
            this.isVerify = isVerify;

            // 콤마로 연결된 문자열을 리스트로 변환
            if (serviceNamesStr != null && !serviceNamesStr.isEmpty()) {
                this.serviceNames = Arrays.stream(serviceNamesStr.split(","))
                        .map(String::trim)
                        .toList();
            } else {
                this.serviceNames = List.of();
            }
        }
    }

    // =================================================================
    // 2. 사용자 상세 정보 (Detail / Modify Info)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class DetailResponse {
        private String loginId;
        private String name;
        private GenderType gender;

        // 정적 팩토리 메서드
        public static DetailResponse from(String loginId, String name, GenderType gender) {
            return DetailResponse.builder()
                    .loginId(loginId)
                    .name(name)
                    .gender(gender)
                    .build();
        }
    }

    // =================================================================
    // 3. 사용자 정보 수정 (Modify)
    // =================================================================
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModifyRequest {
        private Long userId;
        private String loginId;
        private String name;
        private GenderType gender;
    }
}