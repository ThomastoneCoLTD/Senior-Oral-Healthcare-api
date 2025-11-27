package com.kaii.dentix.domain.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SubscriptionOveruseResponse {
    private Long subscriptionId;
    private String planName;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Long totalAmount;
    private List<BillingResponse> overuseList;
}