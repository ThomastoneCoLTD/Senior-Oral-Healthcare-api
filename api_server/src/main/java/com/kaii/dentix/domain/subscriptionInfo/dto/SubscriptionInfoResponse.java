package com.kaii.dentix.domain.subscriptionInfo.dto;

import lombok.*;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionInfoResponse {

    private String organizationName;   // 기관명
    private String planName;           // 구독 플랜명
    private String planCycle;          // monthly / yearly
    private Long price;                // 가격
    private int maxSuccessResponses;   // 최대 응답 수
    private int totalSuccessCount;     // 사용된 응답 수
    private int remainingCount;        // 잔여 응답 수
    private double usageRate;          // 사용률 (%)
    private List<UserUsage> users;     // 사용자별 사용량

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserUsage {
        private Long userId;
        private String userName;
        private String role;
        private int successCount;
    }
}