package com.kaii.dentix.global.security;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/** Resolve privileges from the current database record, never from caller-supplied IDs. */
@Component
@RequiredArgsConstructor
public class AdminAccessGuard {
    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    public Admin currentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            throw new UnauthorizedException("관리자 권한이 필요합니다.");
        }
        try {
            Admin admin = adminRepository.findByIdWithOrganization(Long.valueOf(auth.getName()))
                    .orElseThrow(UnauthorizedException::new);
            if (!admin.isApproved()) throw new UnauthorizedException("슈퍼관리자 승인이 필요합니다.");
            return admin;
        } catch (NumberFormatException exception) {
            throw new UnauthorizedException();
        }
    }

    public void requireSuperAdmin() {
        if (!currentAdmin().isSuperAdmin()) throw new UnauthorizedException("슈퍼 관리자 권한이 필요합니다.");
    }

    public void requireOrganization(Organization organization) {
        requireOrganization(organization == null ? null : organization.getOrganizationId());
    }

    public void requireOrganization(Long organizationId) {
        Admin admin = currentAdmin();
        if (admin.isSuperAdmin()) return;
        if (organizationId == null || admin.getOrganization() == null
                || !Objects.equals(admin.getOrganization().getOrganizationId(), organizationId)) {
            throw new UnauthorizedException("해당 기관에 대한 접근 권한이 없습니다.");
        }
    }
}
