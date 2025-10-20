package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class AdminOrganizationService {
    private final JwtTokenUtil jwtTokenUtil;
    private final AdminRepository adminRepository;

    public Admin getTokenAdmin(HttpServletRequest servletRequest) {
        String token = jwtTokenUtil.getAccessToken(servletRequest);

        UserRole roles = jwtTokenUtil.getRoles(token, TokenType.AccessToken);
        if (!roles.equals(UserRole.ROLE_ADMIN)) throw new UnauthorizedException();

        Long adminId = jwtTokenUtil.getUserId(token, TokenType.AccessToken);
        return adminRepository.findById(adminId).orElseThrow(() -> new NotFoundDataException("존재하지 않는 관리자입니다."));
    }

    @Transactional
    public OrganizationResponse getMyOrganization(HttpServletRequest request) {
        // ✅ 현재 로그인 관리자 ID 추출
        Admin admin = this.getTokenAdmin(request);

        // ✅ 관리자 조회
//        Admin admin = adminRepository.findById(adminId)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        if (admin.getOrganization() == null) {
            throw new BadRequestApiException("아직 등록된 기관이 없습니다.");
        }

        Organization organization = admin.getOrganization();

        // ✅ 응답 DTO 변환
        return OrganizationResponse.builder()
                .organizationId(organization.getOrganizationId())
                .organizationName(organization.getOrganizationName())
                .organizationPhoneNumber(organization.getOrganizationPhoneNumber())
                .subscriptionPlanId(organization.getSubscriptionPlan().getId())
                .subscriptionPlanName(organization.getSubscriptionPlan().getPlanName())
                .subscriptionStartDate(organization.getSubscriptionStartDate())
                .usageResetDate(organization.getUsageResetDate())
                .successCount(organization.getSuccessCount())
                .build();
    }
}
