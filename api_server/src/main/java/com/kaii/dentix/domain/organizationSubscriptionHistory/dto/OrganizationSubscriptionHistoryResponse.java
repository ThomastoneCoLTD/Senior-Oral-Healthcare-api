package com.kaii.dentix.domain.organizationSubscriptionHistory.dto;

import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSubscriptionHistoryResponse {

    private Long historyId;
    private Long subscriptionPlanId;
    private String subscriptionPlanName;
    private String planCycle;
    private Long price;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String reason;

    public static OrganizationSubscriptionHistoryResponse fromEntity(OrganizationSubscriptionHistory history) {
        SubscriptionPlan plan = history.getSubscriptionPlan();

        return OrganizationSubscriptionHistoryResponse.builder()
                .historyId(history.getId())
                .subscriptionPlanId(plan != null ? plan.getId() : null)
                .subscriptionPlanName(plan != null ? plan.getPlanName().name() : null)
                .planCycle(plan != null ? plan.getPlanCycle() : null)
                .price(plan != null ? plan.getPrice() : null)
                .startDate(history.getStartDate())
                .endDate(history.getEndDate())
                .reason(history.getReason())
                .build();
    }
}