package com.kaii.dentix.domain.organization.dto;

import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationSubscriptionHistoryResponse {

    private Long historyId;
    private String subscriptionPlanName;
    private String planCycle;
    private Double price;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String reason;

    public static OrganizationSubscriptionHistoryResponse fromEntity(SubscriptionHistory entity) {
        return OrganizationSubscriptionHistoryResponse.builder()
                .historyId(entity.getId())
                .subscriptionPlanName(entity.getSubscriptionPlan().getPlanName().name()) // ✅ Enum → String 변환
                .planCycle(entity.getSubscriptionPlan().getPlanCycle()) // ✅ Enum → String 변환
                .price(entity.getSubscriptionPlan().getPrice())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .reason(entity.getReason())
                .build();
    }
}