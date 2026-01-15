package com.kaii.dentix.domain.superAdmin.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record SuperAdminAllUserStatisticsResponse(
        long totalUsers,
        long maleUsers,
        long femaleUsers,
        long newUsers7Days,
        List<SuperAdminUserStatisticResponse> organizationStats) {}