package com.kaii.dentix.domain.subscriptionInfo.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionUserUsageResponse {
    private Long userId;
    private String userName;
    private Long successCount;
}