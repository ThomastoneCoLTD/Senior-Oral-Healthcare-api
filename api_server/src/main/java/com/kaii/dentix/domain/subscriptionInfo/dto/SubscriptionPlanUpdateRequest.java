package com.kaii.dentix.domain.subscriptionInfo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionPlanUpdateRequest {
    private String planName;
    private String planCycle; // monthly, yearly
    private Integer planSort;
    private Long price;
    private Integer maxSuccessResponses;
}