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
    private Long price;
    private Integer maxSuccessResponses;

    public static SubscriptionPlanResponse from(SubscriptionPlan plan) {
        return SubscriptionPlanResponse.builder()
                .id(plan.getId())
                .planName(plan.getPlanName())
                .planCycle(plan.getPlanCycle())
                .planSort(plan.getPlanSort())
                .price(plan.getPrice())
                .maxSuccessResponses(plan.getMaxSuccessResponses())
                .build();
    }
}