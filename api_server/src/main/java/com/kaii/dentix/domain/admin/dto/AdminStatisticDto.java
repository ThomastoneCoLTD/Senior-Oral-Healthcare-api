package com.kaii.dentix.domain.admin.dto;

import com.kaii.dentix.domain.type.DatePeriodType;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import lombok.*;

import java.util.List;

public class AdminStatisticDto {

    // =================================================================
    // 1. 통계 검색 요청 (Search Request)
    // =================================================================
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class SearchRequest {
        private Long organizationId;
        private DatePeriodType datePeriodType; // allDatePeriod -> datePeriodType (네이밍 통일)
        private String startDate;
        private String endDate;
    }

    // =================================================================
    // 2. 통계 응답 컴포넌트 (Components) - 재사용 가능
    // =================================================================

    // 가입자 수 통계 (SignUpCount)
    public record SignUpCount(
            Long countAll,
            Long countMan,
            Long countWoman
    ) {}

    // 구강검진 결과 통계 (OralCheckStats)
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class OralCheckStats {
        private int countHealthy;
        private int countGood;
        private int countAttention;
        private int countDanger;
        // 필요 시 합계 계산 메서드 추가 가능
        public int getTotal() {
            return countHealthy + countGood + countAttention + countDanger;
        }
    }

    // 문진표 유형별 통계 (QuestionnaireStats)
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class QuestionnaireStats {
        private int countA;
        private int countB;
        private int countC;
        private int countD;
        private int countE;
        private int countF;
        private int countG;
        private int countH;
        private int countI;
        private int countJ;
        private int countK;
    }

    // =================================================================
    // 3. 메인 통계 응답 (Main Response)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class MainResponse {
        private SignUpCount userSignUpCount;         // 가입자 통계
        private OralCheckResultType averageState;    // 평균 구강 상태
        private int oralCheckCount;                  // 전체 검진 횟수
        private int oralCheckAverage;                // 평균 검진 횟수
        private OralCheckStats oralCheckResultTypeCount; // 검진 결과별 카운트

        private int questionnaireAllCount;           // 전체 문진표 작성 횟수
        private QuestionnaireStats allQuestionnaireResultTypeCount; // 문진표 유형별 카운트
    }

    // =================================================================
    // 4. 기관 및 슈퍼관리자용 통계 응답
    // =================================================================

    // 기관 대시보드용 요약 통계
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class OrgUserStatsResponse {
        private String organizationName;
        private long totalUsers;
        private long maleUsers;
        private long femaleUsers;
        private long newUsers; // 최근 가입
        private List<OralCheckStats> oralCheckStats; // 구강 상태 리스트 (구조에 따라 변경 가능)
    }

    // 슈퍼관리자용 전체 통계
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class SuperAdminTotalResponse {
        private long totalUsers;
        private long maleUsers;
        private long femaleUsers;
        private long newUsers7Days;
        private List<MainResponse> organizationStats; // 기관별 통계 리스트
    }
}