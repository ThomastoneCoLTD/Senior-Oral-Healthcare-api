package com.kaii.dentix.domain.organization.dto;

import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponse {
    private Long organizationId;
    private String organizationName;
    private Long subscriptionPlanId;
    private String subscriptionPlanName;
    private LocalDateTime usageResetDate;
    private LocalDateTime subscriptionStartDate;
    private Integer successCount;
}