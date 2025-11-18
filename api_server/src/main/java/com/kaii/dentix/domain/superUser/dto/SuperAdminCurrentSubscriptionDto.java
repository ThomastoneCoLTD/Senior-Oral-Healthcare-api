package com.kaii.dentix.domain.superUser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SuperAdminCurrentSubscriptionDto {

    private Long organizationId;
    private String organizationName;

    private Long subscriptionPlanId;
    private String subscriptionPlanName;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private boolean active;
}