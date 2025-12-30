package com.kaii.dentix.domain.billing.dto;

import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.billing.util.BillingDescriptionMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 기관 Billing 내역 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class BillingResponse {

    private Long billingId;
    private String subscriptionPlanName;
    private String billingType;
    private String billingStatus;
    private Long amount;
    private LocalDateTime billedAt;
    private Date created;
    private LocalDateTime paidAt;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private String description;

    public static BillingResponse from(Billing b) {
        return BillingResponse.builder()
                .billingId(b.getId())
                .subscriptionPlanName(b.getSubscriptionPlan().getPlanName().name())
                .billingType(b.getBillingType().name())
                .billingStatus(b.getBillingStatus().name())
                .amount(b.getAmount())
                .billedAt(b.getBilledAt())
                .created(b.getCreated())
                .paidAt(b.getPaidAt())
                .periodStart(b.getPeriodStart())
                .periodEnd(b.getPeriodEnd())
                .description(BillingDescriptionMapper.toEnglish(b.getDescription()))
                .build();
    }
}