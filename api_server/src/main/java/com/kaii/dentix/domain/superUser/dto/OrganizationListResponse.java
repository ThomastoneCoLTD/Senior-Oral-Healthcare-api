package com.kaii.dentix.domain.superUser.dto;

import com.kaii.dentix.domain.organization.domain.Organization;
import lombok.Builder;

@Builder
public record OrganizationListResponse(
        Long organizationId,
        String organizationName,
        String organizationPhoneNumber,
        String subscriptionPlanName,
        Boolean active
) {
    public static OrganizationListResponse fromEntity(Organization o) {
        return OrganizationListResponse.builder()
                .organizationId(o.getOrganizationId())
                .organizationName(o.getOrganizationName())
                .organizationPhoneNumber(o.getOrganizationPhoneNumber())
                .subscriptionPlanName(o.getSubscriptionPlan() != null ? o.getSubscriptionPlan().getPlanName().name() : null)
                .active(o.getActive())
                .build();
    }
}