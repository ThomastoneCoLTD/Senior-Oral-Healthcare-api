package com.kaii.dentix.domain.subscription.dto;

import lombok.Builder;

@Builder
public record SubscriptionStatusResponse(
        Long organizationId,
        String organizationName,
        String planName,
        String planCycle,
        Integer planPrice,
        Integer planQuota,
        Integer usedCount,
        Integer remainingCount,
        Double usageRate,
        Boolean overused,
        Integer overusedCount,   // ✅ 초과 사용 횟수 (SubscriptionUsageHistory 기준)
        Integer monthUsedCount   // ✅ 이번 달 사용 횟수 (SubscriptionUsageHistory 기준)
) {}