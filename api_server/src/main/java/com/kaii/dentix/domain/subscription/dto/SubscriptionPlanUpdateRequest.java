package com.kaii.dentix.domain.subscription.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionPlanUpdateRequest {
    private String planName;
    private String planCycle; // monthly, yearly
    private Integer planSort;
    private Double price;
    private Integer maxSuccessResponses;
}