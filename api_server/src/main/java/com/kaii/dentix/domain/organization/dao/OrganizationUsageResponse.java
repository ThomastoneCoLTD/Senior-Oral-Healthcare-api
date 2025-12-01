package com.kaii.dentix.domain.organization.dao;


import com.kaii.dentix.domain.organization.dto.RecentUsage;
import com.kaii.dentix.domain.organization.dto.TopUserUsage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrganizationUsageResponse {

    private String subscriptionPlanName;
    private Integer maxSuccessResponses;
    private Long successCount;
    private Long remainingResponses;
    private Double usageRate;
    // 🔥 사용량 통계 추가
    private Long dailyUsage;
    private Long weeklyUsage;
    private Long monthlyUsage;

    private List<TopUserUsage> topUsers;
    private List<RecentUsage> recentUsages;
    @Getter @Builder @NoArgsConstructor
    @AllArgsConstructor
    public static class TopUserDto {
        private Long userId;
        private String userLoginIdentifier;
        private Integer count;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RecentUsageDto {
        private Long oralCheckId;
        private String userName;
        private String resultType;
        private String userLoginIdentifier;
        private LocalDateTime created;
    }
}