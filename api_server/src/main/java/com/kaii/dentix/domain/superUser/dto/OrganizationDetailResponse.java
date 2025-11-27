package com.kaii.dentix.domain.superUser.dto;


import com.kaii.dentix.domain.organization.domain.Organization;
import lombok.Builder;
/**
 * 슈퍼관리자가 기관 상세 조회 시 반환되는 DTO
 */
@Builder
public record OrganizationDetailResponse(
        Long organizationId,
        String organizationName,
        String organizationEmail,
        String organizationPhoneNumber,
        String subscriptionPlanName,
        Long price,
        String startDate,
        String endDate,
        Boolean active
) {
    public static OrganizationDetailResponse fromEntity(Organization o) {
        return OrganizationDetailResponse.builder()
                .organizationId(o.getOrganizationId())
                .organizationName(o.getOrganizationName())
                .organizationEmail(o.getOrganizationEmail())
                .organizationPhoneNumber(o.getOrganizationPhoneNumber())
                .subscriptionPlanName(
                        o.getSubscriptionPlan() != null
                                ? o.getSubscriptionPlan().getPlanName().name()
                                : null
                )
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
                .active(o.getActive()) // 기관 활성화 여부
                .build();
    }
}