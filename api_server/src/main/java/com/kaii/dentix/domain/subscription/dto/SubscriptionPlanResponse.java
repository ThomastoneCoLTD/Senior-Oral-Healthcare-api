package com.kaii.dentix.domain.subscription.dto;

import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriptionPlanResponse {
    private Long id;
    private String planName;
    private String planCycle;
    private Integer planSort;
    private Double price;
    private Integer maxSuccessResponses;
    private Boolean customSurveyEnabled;
    private Boolean reportExportEnabled;
    private Integer overuseUnitPrice;

    public static SubscriptionPlanResponse from(SubscriptionPlan plan) {
        return SubscriptionPlanResponse.builder()
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