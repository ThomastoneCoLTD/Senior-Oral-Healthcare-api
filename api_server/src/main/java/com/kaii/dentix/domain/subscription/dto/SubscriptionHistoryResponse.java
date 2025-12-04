package com.kaii.dentix.domain.subscription.dto;

import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import lombok.Builder;

/**
 * ✅ 기관 구독 이력 응답 DTO
 */
@Builder
public record SubscriptionHistoryResponse(
        Long subscriptionHistoryId,
        String planName,
        String planCycle,
        Long price,
        String startDate,
        String endDate,
        String reason,
        String status
) {
    public static SubscriptionHistoryResponse fromEntity(OrganizationSubscriptionHistory h) {
        SubscriptionPlan plan = h.getSubscriptionPlan();

        return SubscriptionHistoryResponse.builder()
                .subscriptionHistoryId(h.getId())
                .planName(plan != null ? plan.getPlanName().name() : null)
                .planCycle(plan != null ? plan.getPlanCycle() : null)   // ← 여기!
                .price(plan != null ? plan.getPrice() : null)
                .startDate(h.getStartDate() != null ? h.getStartDate().toString() : null)
                .endDate(h.getEndDate() != null ? h.getEndDate().toString() : null)
                .reason(h.getReason())
                .status(h.getStatus() != null ? h.getStatus().name() : null)
                .build();
    }
}
