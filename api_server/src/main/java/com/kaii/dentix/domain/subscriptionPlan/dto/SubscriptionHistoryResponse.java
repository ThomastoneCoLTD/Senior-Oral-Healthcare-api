package com.kaii.dentix.domain.subscriptionPlan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionHistoryResponse {
    private String planName;           // 구독 플랜명
    private String planCycle;          // monthly / yearly
    private Long price;                // 요금
    private LocalDateTime startDate;   // 구독 시작일
    private LocalDateTime endDate;
}