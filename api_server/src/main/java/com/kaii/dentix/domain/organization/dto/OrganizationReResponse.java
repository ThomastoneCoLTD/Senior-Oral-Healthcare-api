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
    public static OrganizationReResponse from(
            Organization org,
            OrganizationSubscription subscription,
            List<OrganizationSubscriptionHistory> histories
    ) {
        SubscriptionPlan plan =
                subscription != null ? subscription.getSubscriptionPlan() : null;

        int max =
                plan != null && plan.getMaxSuccessResponses() != null
                        ? plan.getMaxSuccessResponses()
                        : 0;

        int success =
                subscription != null && subscription.getSuccessCount() != null
                        ? subscription.getSuccessCount()
                        : 0;

        int remaining = Math.max(max - success, 0);
        double usageRate = (max > 0) ? (success * 100.0 / max) : 0.0;

        return OrganizationReResponse.builder()
                // 기관 정보
                .organizationId(org.getOrganizationId())
                .organizationName(org.getOrganizationName())
                .organizationEmail(org.getOrganizationEmail())
                .organizationPhoneNumber(org.getOrganizationPhoneNumber())

                // ✅ 현재 구독 (Subscription 기준)
                .subscriptionPlanId(plan != null ? plan.getId() : null)
                .subscriptionPlanName(plan != null ? plan.getPlanName().name() : null)
                .subscriptionStartDate(subscription.getSubscriptionStartDate())
                .subscriptionEndDate(subscription.getSubscriptionEndDate())
                .subscriptionStatus("ACTIVE") // 필요하면 enum로

                // 사용량
                .successCount(success)
                .remainingResponses(remaining)
                .usageRate(usageRate)

                // 플랜 옵션
                .price(plan != null ? plan.getPrice() : null)
                .customSurveyEnabled(plan != null ? plan.getCustomSurveyEnabled() : null)
                .reportExportEnabled(plan != null ? plan.getReportExportEnabled() : null)
                .overuseUnitPrice(plan != null ? plan.getOveruseUnitPrice() : null)
                .maxSuccessResponses(max)

                .build();
    }
}