package com.kaii.dentix.domain.superUser.dto;

import com.kaii.dentix.domain.admin.dto.superAdmin.SuperAdminUserStatisticResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record SuperAdminAllUserStatisticsResponse(
        long totalUsers,
        long maleUsers,
        long femaleUsers,
        long newUsers7Days,
        List<SuperAdminUserStatisticResponse> organizationStats) {}