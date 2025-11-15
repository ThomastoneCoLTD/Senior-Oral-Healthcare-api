package com.kaii.dentix.domain.organization.dto;

import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.kaii.dentix.domain.organization.domain.Organization;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponse {

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
    private Double price;
    private Boolean reportExportEnabled;
    private Boolean customSurveyEnabled;
    private Integer overuseUnitPrice;
    private Integer maxSuccessResponses;
    public static OrganizationResponse from(Organization org) {
        OrganizationSubscription sub = org.getOrganizationSubscription();
        var plan = (sub != null) ? sub.getSubscriptionPlan() : null;

        return OrganizationResponse.builder()
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

                // 🔥 신규 필드들
                .price(plan != null ? plan.getPrice() : null)
                .customSurveyEnabled(plan != null ? plan.getCustomSurveyEnabled() : null)
                .reportExportEnabled(plan != null ? plan.getReportExportEnabled() : null)
                .overuseUnitPrice(plan != null ? plan.getOveruseUnitPrice() : null)
                .maxSuccessResponses(plan != null ? plan.getMaxSuccessResponses() : null)
                .build();
    }
}