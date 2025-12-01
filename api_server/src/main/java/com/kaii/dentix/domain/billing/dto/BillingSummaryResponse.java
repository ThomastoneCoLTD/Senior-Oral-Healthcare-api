package com.kaii.dentix.domain.billing.dto;

import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.type.BillingType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BillingSummaryResponse {

    private Long billingId;
    private BillingType billingType;
    private long amount;
    private LocalDateTime billedAt;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    private String description;

    public static BillingSummaryResponse from(Billing billing) {
        return BillingSummaryResponse.builder()
                .billingId(billing.getId())
                .billingType(billing.getBillingType())
                .amount(billing.getAmount())
                .billedAt(billing.getBilledAt())
                .periodStart(billing.getPeriodStart())
                .periodEnd(billing.getPeriodEnd())
                .description(billing.getDescription())
                .build();
    }
}