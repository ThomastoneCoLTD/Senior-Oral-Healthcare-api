package com.kaii.dentix.domain.organization.dto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationRequest {
    private String organizationName;
    private String organizationPhoneNumber;
    private Long subscriptionPlanId;
}
