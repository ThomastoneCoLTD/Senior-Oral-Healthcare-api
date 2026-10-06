package com.kaii.dentix.global.security;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminAccessGuardTest {
    private final AdminRepository repository = mock(AdminRepository.class);
    private final AdminAccessGuard guard = new AdminAccessGuard(repository);
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private void login(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "7", "", List.of(new SimpleGrantedAuthority(role))));
    }
    private void admin(YnType superAdmin, Long organizationId) {
        when(repository.findByIdWithOrganization(7L)).thenReturn(Optional.of(Admin.builder().adminId(7L)
                .adminIsSuper(superAdmin).organization(organizationId == null ? null : Organization.builder().organizationId(organizationId).build()).build()));
    }
    @Test void ordinaryAdminCanReadOwnOrganizationButNotAnotherOrUnassigned() {
        login("ROLE_ADMIN"); admin(YnType.N, 2L);
        assertThatCode(() -> guard.requireOrganization(2L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.requireOrganization(3L)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> guard.requireOrganization((Long) null)).isInstanceOf(UnauthorizedException.class);
    }
    @Test void staleSuperAuthorityDoesNotOverrideDatabaseDemotion() {
        login("ROLE_SUPER_ADMIN"); admin(YnType.N, 2L);
        assertThatThrownBy(guard::requireSuperAdmin).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> guard.requireOrganization(3L)).isInstanceOf(UnauthorizedException.class);
    }
    @Test void superAdminCanAccessUnassignedAndOtherOrganizations() {
        login("ROLE_SUPER_ADMIN"); admin(YnType.Y, null);
        assertThatCode(() -> guard.requireOrganization((Long) null)).doesNotThrowAnyException();
        assertThatCode(() -> guard.requireOrganization(3L)).doesNotThrowAnyException();
    }
    @Test void userWithCollidingNumericIdCannotBecomeAnAdministrator() {
        login("ROLE_USER");
        assertThatThrownBy(guard::currentAdmin).isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(repository);
    }

    @Test void pendingAccountCannotUseAnOldAdminAuthority() {
        login("ROLE_ADMIN");
        when(repository.findByIdWithOrganization(7L)).thenReturn(Optional.of(Admin.builder().adminId(7L)
                .adminIsSuper(YnType.N).approvalStatus(com.kaii.dentix.domain.admin.domain.AdminApprovalStatus.PENDING).build()));
        assertThatThrownBy(guard::currentAdmin).isInstanceOf(UnauthorizedException.class);
    }
}
