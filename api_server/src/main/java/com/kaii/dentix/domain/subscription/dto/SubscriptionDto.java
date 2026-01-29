package com.kaii.dentix.domain.subscription.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.subscription.domain.SubscriptionUsage;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class SubscriptionDto {

    // =================================================================
    // 1. 구독 플랜 응답 (PlanResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PlanResponse {
        private Long id;
        private String planName;
        private String planCycle;
        private Integer planSort;
        private Long price;
        private Integer maxSuccessResponses;
        private Boolean customSurveyEnabled;
        private Boolean reportExportEnabled;
        private Integer overuseUnitPrice;

        public static PlanResponse from(SubscriptionPlan plan) {
            return PlanResponse.builder()
                    .id(plan.getId())
                    .planName(plan.getPlanName().name())
                    .planCycle(plan.getPlanCycle())
                    .planSort(plan.getPlanSort())
                    .price(plan.getPrice())
                    .maxSuccessResponses(plan.getMaxSuccessResponses())
                    .customSurveyEnabled(plan.getCustomSurveyEnabled())
                    .reportExportEnabled(plan.getReportExportEnabled())
                    .overuseUnitPrice(plan.getOveruseUnitPrice())
                    .build();
        }
    }

    // =================================================================
    // 2. 구독 정보 상세 응답 (InfoResponse) - 대시보드/현황용
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class InfoResponse {
        private Long id;
        private String organizationName;
        private String planName;
        private String planCycle;
        private Long price;

        // 제공량 / 사용량 / 잔여량 / 사용률
        private Integer maxSuccessResponses;
        private Integer totalSuccessCount;
        private Integer remainingCount;
        private Double usageRate;
        private Integer planSort;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime subscriptionStartDate;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime usageResetDate;

        private List<UserUsage> users;

        @Getter @Builder
        @NoArgsConstructor @AllArgsConstructor
        public static class UserUsage {
            private Long userId;
            private String userName;
            private Integer successCount;
        }
    }

    // =================================================================
    // 3. 구독 이력 응답 (HistoryResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class HistoryResponse {
        private Long subscriptionHistoryId;
        private String planName;
        private String planCycle;
        private Long price;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        private LocalDateTime startDate;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        private LocalDateTime endDate;

        private String reason;
        private String status;

        public static HistoryResponse fromEntity(OrganizationSubscriptionHistory h) {
            SubscriptionPlan plan = h.getSubscriptionPlan();
            return HistoryResponse.builder()
                    .subscriptionHistoryId(h.getId())
                    .planName(plan != null ? plan.getPlanName().name() : null)
                    .planCycle(plan != null ? plan.getPlanCycle() : null)
                    .price(plan != null ? plan.getPrice() : null)
                    .startDate(h.getStartDate())
                    .endDate(h.getEndDate())
                    .reason(h.getReason())
                    .build();
        }
    }

    // =================================================================
    // 4. 구독 플랜 수정 요청 (PlanUpdateRequest)
    // =================================================================
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class PlanUpdateRequest {
        private String planName;
        private String planCycle; // monthly, yearly
        private Integer planSort;
        private Long price;
        private Integer maxSuccessResponses;
    }

    // =================================================================
    // 5. 구독 상태 응답 (StatusResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class StatusResponse {
        private Long organizationId;
        private String organizationName;
        private String planName;
        private String planCycle;
        private Long planPrice;
        private Integer planQuota;
        private Integer usedCount;
        private Integer remainingCount;
        private Double usageRate;
        private Boolean overused;
        private Integer overusedCount;
        private Integer monthUsedCount;
    }
}