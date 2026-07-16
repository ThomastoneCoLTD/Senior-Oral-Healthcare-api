package com.kaii.dentix.domain.organization;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.application.DaeguDefaultOrganizationService;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DaeguDefaultOrganizationServiceTest {

    private OrganizationRepository organizationRepository;
    private AdminRepository adminRepository;
    private DaeguDefaultOrganizationService service;

    @BeforeEach
    void setUp() {
        organizationRepository = mock(OrganizationRepository.class);
        adminRepository = mock(AdminRepository.class);
        service = new DaeguDefaultOrganizationService(organizationRepository, adminRepository);
    }

    @Test
    void getTokenAdminOrganizationReturnsTokenAdminOrganization() {
        Organization organization = Organization.builder()
                .organizationId(9L)
                .organizationName("Token Admin Organization")
                .build();
        Admin tokenAdmin = Admin.builder()
                .adminLoginIdentifier("tokenadmin")
                .organization(organization)
                .build();
        when(adminRepository.findByAdminLoginIdentifier("tokenadmin"))
                .thenReturn(Optional.of(tokenAdmin));

        Organization response = service.getTokenAdminOrganization();

        assertThat(response).isSameAs(organization);
    }

    @Test
    void getTokenAdminOrganizationFailsWhenTokenAdminHasNoOrganization() {
        Admin tokenAdmin = Admin.builder()
                .adminLoginIdentifier("tokenadmin")
                .organization(null)
                .build();
        when(adminRepository.findByAdminLoginIdentifier("tokenadmin"))
                .thenReturn(Optional.of(tokenAdmin));

        assertThatThrownBy(() -> service.getTokenAdminOrganization())
                .isInstanceOf(NotFoundDataException.class)
                .hasMessageContaining("tokenadmin 계정에 소속 기관이 없습니다.");
    }

    @Test
    void getTokenAdminOrganizationFailsWhenTokenAdminDoesNotExist() {
        when(adminRepository.findByAdminLoginIdentifier("tokenadmin"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTokenAdminOrganization())
                .isInstanceOf(NotFoundDataException.class)
                .hasMessageContaining("tokenadmin 계정을 찾을 수 없습니다.");
    }
}
