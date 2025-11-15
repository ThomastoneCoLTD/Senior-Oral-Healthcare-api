package com.kaii.dentix.domain.admin.dto;

import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.dto.OrganizationSubscriptionResponse;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.dto.TokenDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter @SuperBuilder
@AllArgsConstructor @NoArgsConstructor
public class AdminLoginDto extends TokenDto {

    private YnType isFirstLogin;

    private Long adminId;

    private String adminName;

    private YnType adminIsSuper;
    private Long organizationId;
    private String organizationName;
    private OrganizationSubscriptionResponse organizationSubscription;

    public AdminLoginDto(Admin admin) {
        this.adminId = admin.getAdminId();
        this.adminName = admin.getAdminName();
        this.adminIsSuper = admin.getAdminIsSuper();
//        this.isFirstLogin = admin.getIsFirstLogin();

        if (admin.getOrganization() != null) {
            this.organizationId = admin.getOrganization().getOrganizationId();
            this.organizationName = admin.getOrganization().getOrganizationName();
        }
    }
}
