package com.kaii.dentix.domain.organization.dto;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationReResponse {

    private Long organizationId;
    private String organizationName;
    private String organizationEmail;
    private String organizationPhoneNumber;

    //현재 구독 정보 (history 기준)
    private Long subscriptionPlanId;
    private String subscriptionPlanName;
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate; // 활성 시 null
    private String subscriptionStatus;         // 계산값 (ACTIVE / EXPIRED)

    private Integer successCount;
    private Integer remainingResponses;
    private Double usageRate;

    // 플랜 옵션
    private Long price;
    private Boolean reportExportEnabled;
    private Boolean customSurveyEnabled;
    private Integer overuseUnitPrice;
    private Integer maxSuccessResponses;

    public static OrganizationReResponse from(
            Organization org,
            OrganizationSubscriptionHistory activeHistory,
            List<OrganizationSubscriptionHistory> histories
    ) {
        SubscriptionPlan plan = activeHistory.getSubscriptionPlan();

        int max = plan.getMaxSuccessResponses();
        int success = activeHistory.getSuccessCount();
        int remaining = activeHistory.getRemainingResponses();
        double usageRate = (max > 0) ? (success * 100.0 / max) : 0.0;

        return OrganizationReResponse.builder()
                // 기관 정보
                .organizationId(org.getOrganizationId())
                .organizationName(org.getOrganizationName())
                .organizationEmail(org.getOrganizationEmail())
                .organizationPhoneNumber(org.getOrganizationPhoneNumber())

                // 현재 구독 (history 기반)
                .subscriptionPlanId(plan.getId())
                .subscriptionPlanName(plan.getPlanName().name())
                .subscriptionStartDate(activeHistory.getStartDate())
                .subscriptionEndDate(activeHistory.getEndDate())

                // 사용량
                .successCount(success)
                .remainingResponses(remaining)
                .usageRate(usageRate)

                // 플랜 옵션
                .price(plan.getPrice())
                .customSurveyEnabled(plan.getCustomSurveyEnabled())
                .reportExportEnabled(plan.getReportExportEnabled())
                .overuseUnitPrice(plan.getOveruseUnitPrice())
                .maxSuccessResponses(max)

                .build();
    }
}