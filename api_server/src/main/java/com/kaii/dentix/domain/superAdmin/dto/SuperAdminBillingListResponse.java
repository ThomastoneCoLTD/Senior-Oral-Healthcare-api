package com.kaii.dentix.domain.superAdmin.dto;

import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.BillingType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SuperAdminBillingListResponse {
    private Long organizationId;
    private String organizationName;
    private List<BillingSummary> billings;

    @Getter
    @Builder
    public static class BillingSummary {
        private Long billingId;
        private BillingType billingType;
        private Long amount;
        private LocalDateTime billedAt;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
        private String description;
    }

    public static SuperAdminBillingListResponse from(Organization org, List<Billing> list) {
        return SuperAdminBillingListResponse.builder()
                .organizationId(org.getOrganizationId())
                .organizationName(org.getOrganizationName())
                .billings(
                        list.stream().map(b -> BillingSummary.builder()
                                .billingId(b.getId())
                                .billingType(b.getBillingType())
                                .amount(b.getAmount())
                                .billedAt(b.getBilledAt())
                                .periodStart(b.getPeriodStart())
                                .periodEnd(b.getPeriodEnd())
                                .description(b.getDescription())
                                .build()
                        ).toList()
                )
                .build();
    }
}
