package com.kaii.dentix.domain.organization.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class OrganizationDto {

    // 1. Request
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "기관명은 필수입니다.")
        private String organizationName;
        @NotBlank(message = "기관 연락처는 필수입니다.")
        @Pattern(regexp = "^[0-9]{9,15}$", message = "전화번호는 숫자만 입력 가능합니다.")
        private String organizationPhoneNumber;
        private Long subscriptionPlanId;
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        private String organizationEmail;
    }

    // 2. UpdateRequest
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "기관명은 필수입니다.")
        private String organizationName;
        @NotBlank(message = "기관 연락처는 필수입니다.")
        @Pattern(regexp = "^[0-9]{9,15}$", message = "전화번호는 숫자만 입력 가능합니다.")
        private String organizationPhoneNumber;
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        private String organizationEmail;
    }

    // 3. Response (상세)
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private Long organizationId;
        private String organizationName;
        private String organizationEmail;
        private String organizationPhoneNumber;

        private Long subscriptionPlanId;
        private String subscriptionPlanName;
        private LocalDateTime subscriptionStartDate;
        private LocalDateTime subscriptionEndDate;
        private String subscriptionStatus;

        private Integer successCount;
        private Integer remainingResponses;
        private Double usageRate;
        private Long price;
        private Boolean reportExportEnabled;
        private Boolean customSurveyEnabled;
        private Integer overuseUnitPrice;
        private Integer maxSuccessResponses;

        private List<SubscriptionHistoryResponse> subscriptionHistories;

        public static Response from(Organization org) {
            OrganizationSubscription sub = org.getOrganizationSubscription();
            SubscriptionPlan plan = (sub != null) ? sub.getSubscriptionPlan() : null;

            return Response.builder()
                    .organizationId(org.getOrganizationId())
                    .organizationName(org.getOrganizationName())
                    .organizationEmail(org.getOrganizationEmail())
                    .organizationPhoneNumber(org.getOrganizationPhoneNumber())
                    .subscriptionPlanId(plan != null ? plan.getId() : null)
                    .subscriptionPlanName(plan != null ? plan.getPlanName().name() : null)
                    .subscriptionStartDate(sub != null ? sub.getSubscriptionStartDate() : null)
                    .subscriptionEndDate(sub != null ? sub.getSubscriptionEndDate() : null)
                    .subscriptionStatus(sub != null ? sub.getStatus().name() : null)
                    .successCount(sub != null ? sub.getSuccessCount() : null)
                    .remainingResponses(sub != null ? sub.getRemainingResponses() : null)
                    .usageRate(sub != null ? sub.getUsageRate() : null)
                    .price(plan != null ? plan.getPrice() : null)
                    .customSurveyEnabled(plan != null ? plan.getCustomSurveyEnabled() : null)
                    .reportExportEnabled(plan != null ? plan.getReportExportEnabled() : null)
                    .overuseUnitPrice(plan != null ? plan.getOveruseUnitPrice() : null)
                    .maxSuccessResponses(plan != null ? plan.getMaxSuccessResponses() : null)
                    .build();
        }
    }

    // =================================================================
    // 4. 기관 구독 정보 응답
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class SubscriptionResponse {
        private Long organizationId;
        private String organizationName;
        private String organizationPhoneNumber;

        private Long subscriptionPlanId;
        private String subscriptionPlanName;
        private String planCycle;
        private Long price;
        private int maxSuccessResponses;

        private int successCount;
        private int remainingCount;
        private double usageRate;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        private LocalDate subscriptionStartDate;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        private LocalDate subscriptionEndDate;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        private LocalDate usageResetDate;

        public static SubscriptionResponse fromEntity(Long organizationId, String organizationName, String organizationPhoneNumber,
                                                      SubscriptionPlan plan, int successCount, LocalDate start, LocalDate end, LocalDate reset) {
            int remaining = Math.max(plan.getMaxSuccessResponses() - successCount, 0);
            double usageRate = 0.0;
            if (plan.getMaxSuccessResponses() > 0) {
                usageRate = ((double) successCount / plan.getMaxSuccessResponses()) * 100;
                if (remaining == 0) usageRate = 100.0;
            }
            usageRate = Math.round(usageRate * 10) / 10.0;

            return SubscriptionResponse.builder()
                    .organizationId(organizationId)
                    .organizationName(organizationName)
                    .organizationPhoneNumber(organizationPhoneNumber)
                    .subscriptionPlanId(plan.getId())
                    .subscriptionPlanName(plan.getPlanName().name())
                    .planCycle(plan.getPlanCycle())
                    .price(plan.getPrice())
                    .maxSuccessResponses(plan.getMaxSuccessResponses())
                    .successCount(successCount)
                    .remainingCount(remaining)
                    .usageRate(usageRate)
                    .subscriptionStartDate(start)
                    .subscriptionEndDate(end)
                    .usageResetDate(reset)
                    .build();
        }
    }

    // =================================================================
    // 5. 구독 변경 요청 (SubscriptionChangeRequest)
    // =================================================================
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class SubscriptionChangeRequest {
        private Long organizationId;
        private Long newSubscriptionPlanId;
    }

    // =================================================================
    // 6. 기관 사용량 통계 (UsageResponse & Inner DTOs)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class UsageResponse {
        private String subscriptionPlanName;
        private Integer maxSuccessResponses;
        private Long successCount;
        private Long remainingResponses;
        private Double usageRate;

        private Long dailyUsage;
        private Long weeklyUsage;
        private Long monthlyUsage;

        private List<TopUser> topUsers;
        private List<RecentUsage> recentUsages;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class TopUser {
        private Long userId;
        private String userName;
        private String userLoginIdentifier;
        private Long count; // UsageCount
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class RecentUsage {
        private Long oralCheckId;
        private String userName;
        private String userLoginIdentifier;
        private OralCheckResultType resultType;
        private Date created;
    }

    // =================================================================
    // 7. 기관 이력 (HistoryResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class HistoryResponse {
        private Long historyId;
        private String fieldName;
        private String beforeValue;
        private String afterValue;
        private Long modifiedByAdminId;
        private String modifiedAt;
    }

    // =================================================================
    // 8. 구독 이력 상세
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class SubscriptionHistoryResponse {
        private Long historyId;
        private Long subscriptionPlanId;
        private String subscriptionPlanName;
        private String planCycle;
        private Long price;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String reason;

        public static SubscriptionHistoryResponse fromEntity(OrganizationSubscriptionHistory entity) {
            SubscriptionPlan plan = entity.getSubscriptionPlan();
            return SubscriptionHistoryResponse.builder()
                    .historyId(entity.getId())
                    .subscriptionPlanId(plan != null ? plan.getId() : null)
                    .subscriptionPlanName(plan != null ? plan.getPlanName().name() : null)
                    .planCycle(plan != null ? plan.getPlanCycle() : null)
                    .price(plan != null ? plan.getPrice() : null)
                    .startDate(entity.getStartDate())
                    .endDate(entity.getEndDate())
                    .reason(entity.getReason())
                    .build();
        }
    }
}