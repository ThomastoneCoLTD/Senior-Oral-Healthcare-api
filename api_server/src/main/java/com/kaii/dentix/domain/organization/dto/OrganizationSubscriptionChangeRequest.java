package com.kaii.dentix.domain.organization.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationSubscriptionChangeRequest {
    private Long organizationId;
    private Long newSubscriptionPlanId;
//    private String reason; // 변경 사유 (선택)
}
