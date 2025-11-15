package com.kaii.dentix.domain.billing.dto;

import com.kaii.dentix.domain.billing.domain.Billing;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * ✅ Billing 상세 응답 DTO
 */
@Getter
@Builder
public class BillingDetailResponse {

    private Long billingId;
    private String organizationName;
    private String planName;
    private String billingType;
    private String billingStatus;
    private Double amount;
    private LocalDateTime billedAt;
    private LocalDateTime paidAt;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private String description;
    private String paymentRef;

    public static BillingDetailResponse from(Billing billing) {
        return BillingDetailResponse.builder()
                .billingId(billing.getId())
                .organizationName(billing.getOrganization().getOrganizationName())
                .planName(billing.getSubscriptionPlan().getPlanName().name())
                .billingType(billing.getBillingType().name())
                .billingStatus(billing.getBillingStatus().name())
                .amount(billing.getAmount())
                .billedAt(billing.getBilledAt())
                .paidAt(billing.getPaidAt())
                .periodStart(billing.getPeriodStart())
                .periodEnd(billing.getPeriodEnd())
                .description(billing.getDescription())
                .paymentRef(billing.getPaymentRef())
                .build();
    }
}