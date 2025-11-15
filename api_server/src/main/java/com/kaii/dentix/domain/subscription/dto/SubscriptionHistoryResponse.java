package com.kaii.dentix.domain.subscription.dto;

import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ✅ 기관 구독 이력 응답 DTO
 */
@Builder
public record SubscriptionHistoryResponse(
        Long subscriptionHistoryId,
        String planName,
        String startDate,
        String endDate,
        String reason
//        String status
) {
    public static SubscriptionHistoryResponse fromEntity(SubscriptionHistory s) {
        return SubscriptionHistoryResponse.builder()
                .subscriptionHistoryId(s.getId())
                .planName(s.getSubscriptionPlan() != null ? s.getSubscriptionPlan().getPlanName().name() : null)
                .startDate(s.getStartDate() != null ? s.getStartDate().toString() : null)
                .endDate(s.getEndDate() != null ? s.getEndDate().toString() : null)
                .reason(s.getReason())
                .build();
    }
}
