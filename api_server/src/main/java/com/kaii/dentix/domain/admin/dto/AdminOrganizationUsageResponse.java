package com.kaii.dentix.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrganizationUsageResponse {
    private Long organizationId;
    private String organizationName;
    private String planName;

    private Integer maxSuccessResponses;
    private Integer successCount;
    private Integer remainingCount;
    private Double usageRate; // 사용률 (%)
}
