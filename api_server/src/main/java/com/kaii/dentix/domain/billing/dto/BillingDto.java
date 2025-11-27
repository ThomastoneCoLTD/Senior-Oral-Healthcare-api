package com.kaii.dentix.domain.billing.dto;

import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.type.BillingStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class BillingDto {
    private Long billingId;
    private Long organizationId;
    private String organizationName;
    private String planName;
    private String billingType;
    private String billingStatus;
    private Long amount;
    private LocalDateTime billedAt;
    private LocalDateTime paidAt;

    public static BillingDto from(Billing billing) {
        return BillingDto.builder()
                .billingId(billing.getId())
                .organizationId(billing.getOrganization().getOrganizationId())
                .organizationName(billing.getOrganization().getOrganizationName())
                .planName(billing.getSubscriptionPlan().getPlanName().name())
                .billingType(billing.getBillingType().name())
                .billingStatus(billing.getBillingStatus().name())
                .amount(billing.getAmount())
                .billedAt(billing.getBilledAt())
                .paidAt(billing.getPaidAt())
                .build();
    }
}
