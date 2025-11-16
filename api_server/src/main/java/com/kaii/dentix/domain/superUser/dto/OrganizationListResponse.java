package com.kaii.dentix.domain.superUser.dto;

import com.kaii.dentix.domain.organization.domain.Organization;
import lombok.Builder;

@Builder
public record OrganizationListResponse(
        Long organizationId,
        String organizationName,
        String organizationPhoneNumber,
        String subscriptionPlanName,
        Double price,
        String startDate,
        String endDate,
        Boolean active
) {
    public static OrganizationListResponse fromEntity(Organization o) {
        return OrganizationListResponse.builder()
                .organizationId(o.getOrganizationId())
                .organizationName(o.getOrganizationName())
                .organizationPhoneNumber(o.getOrganizationPhoneNumber())
                .subscriptionPlanName(o.getSubscriptionPlan() != null ? o.getSubscriptionPlan().getPlanName().name() : null)
                .price(
                        o.getSubscriptionPlan() != null
                                ? o.getSubscriptionPlan().getPrice()
                                : null
                )
                .startDate(
                        o.getSubscriptionStartDate() != null
                                ? o.getSubscriptionStartDate().toString()
                                : null
                )
                .endDate(
                        o.getSubscriptionEndDate() != null
                                ? o.getSubscriptionEndDate().toString()
                                : null
                )
                .active(o.getActive())
                .build();
    }
}