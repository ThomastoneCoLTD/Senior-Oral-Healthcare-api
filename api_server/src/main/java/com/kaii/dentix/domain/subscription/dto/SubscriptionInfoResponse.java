package com.kaii.dentix.domain.subscription.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionInfoResponse {
    private Long id;
    private String organizationName;
    private String planName;
    private String planCycle;
    private Long price;

    // 제공량 / 사용량 / 잔여량 / 사용률
    private Integer maxSuccessResponses;
    private Integer totalSuccessCount;
    private Integer remainingCount;
    private Double usageRate;
    private Integer planSort;
    // ✅ 구독 시작일 & 갱신일
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime usageResetDate;

    private List<UserUsage> users;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserUsage {
        private Long userId;
        private String userName;
        private Integer successCount;
    }
}