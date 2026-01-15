package com.kaii.dentix.domain.superAdmin.dto;

import lombok.*;

import java.util.List;

public class SuperAdminStatisticDto {

    // =================================================================
    // 슈퍼관리자용 기관별 사용자 통계 요약 (기존 SuperAdminUserStatisticResponse 대체)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor
    public static class OrgUserStats {
        private Long organizationId;
        private String organizationName;
        private Long totalUsers;
        private Long maleUsers;
        private Long femaleUsers;
        private Long newUsers; // 최근 1개월 가입자 수

        // QueryDSL Projections.constructor 사용을 위한 생성자
        public OrgUserStats(Long organizationId, String organizationName, Long totalUsers, Long maleUsers, Long femaleUsers, Long newUsers) {
            this.organizationId = organizationId;
            this.organizationName = organizationName;
            this.totalUsers = totalUsers;
            this.maleUsers = maleUsers;
            this.femaleUsers = femaleUsers;
            this.newUsers = newUsers;
        }
    }

    // =================================================================
    // (신규) 2. 슈퍼관리자용 전체 사용자 통계 (기존 SuperAdminAllUserStatisticsResponse 대체)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class TotalUserStats {
        private Long totalUsers;
        private Long maleUsers;
        private Long femaleUsers;
        private Long newUsers7Days; // 최근 7일 가입자
        private List<OrgUserStats> organizationStats; // 기관별 통계 리스트 포함
    }
}