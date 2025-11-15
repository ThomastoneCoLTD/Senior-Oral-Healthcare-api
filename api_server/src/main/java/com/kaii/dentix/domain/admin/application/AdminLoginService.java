package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminFindPasswordDto;
import com.kaii.dentix.domain.admin.dto.AdminLoginDto;
import com.kaii.dentix.domain.admin.dto.AdminResetPasswordRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminFindPasswordRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminLoginRequest;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organization.dto.OrganizationSubscriptionResponse;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;


@Service
@RequiredArgsConstructor
public class AdminLoginService {
    private final JwtTokenUtil jwtTokenUtil;

    private final AdminRepository adminRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     *  관리자 로그인
     */
    @Transactional
    public AdminLoginDto login(AdminLoginRequest request) {

        // 관리자 조회
        Admin admin = adminRepository.findByAdminLoginIdentifier(request.getAdminLoginIdentifier())
                .orElseThrow(() -> new UnauthorizedException("입력하신 정보가 일치하지 않습니다. 다시 확인해주세요."));

        // ✅ 최초 로그인 여부 확인
        YnType isFirstLogin = admin.getAdminLastLoginDate() == null ? YnType.Y : YnType.N;

        // ✅ 비밀번호가 비어있는 경우 (최초 로그인)
//        if (admin.getAdminPassword() == null || admin.getAdminPassword().isEmpty()) {
//            isFirstLogin = YnType.Y;
//            admin.updatePassword(passwordEncoder, SecurityUtil.defaultPassword);
//        }

        // ✅ 비밀번호 검증
        if (!passwordEncoder.matches(request.getAdminPassword(), admin.getAdminPassword())) {
            throw new UnauthorizedException("입력하신 정보가 일치하지 않습니다. 다시 확인해주세요.");
        }

        // ✅ 토큰 생성
        String accessToken = jwtTokenUtil.createToken(admin, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(admin, TokenType.RefreshToken);

        // ✅ 리프레시 토큰 저장
        admin.updateAdminLogin(refreshToken);

        // ✅ 기관이 연결되어 있는지 확인
        Organization org = admin.getOrganization();
        OrganizationSubscriptionResponse subscriptionResponse = null;
        Long organizationId = null;
        String organizationName = null;

        if (org != null) {
            organizationId = org.getOrganizationId();
            organizationName = org.getOrganizationName();

            // ✅ 기관 구독 정보 조회
            OrganizationSubscription subscription = subscriptionRepository
                    .findByOrganization_OrganizationId(organizationId)
                    .orElse(null);

            if (subscription != null) {
                SubscriptionPlan plan = subscription.getSubscriptionPlan();

                // ⚙️ LocalDate 변환
                LocalDate startDate = subscription.getSubscriptionStartDate() != null
                        ? subscription.getSubscriptionStartDate().toLocalDate()
                        : null;
                LocalDate endDate = subscription.getSubscriptionEndDate() != null
                        ? subscription.getSubscriptionEndDate().toLocalDate()
                        : null;
                LocalDate resetDate = subscription.getUsageResetDate() != null
                        ? subscription.getUsageResetDate().toLocalDate()
                        : null;

                // ✅ DTO 변환
                subscriptionResponse = OrganizationSubscriptionResponse.fromEntity(
                        org.getOrganizationId(),
                        org.getOrganizationName(),
                        org.getOrganizationPhoneNumber(),
                        plan,
                        subscription.getSuccessCount(),
                        startDate,
                        endDate,
                        resetDate
                );
            }
        }

        // ✅ 최종 응답 구성
        return AdminLoginDto.builder()
                .isFirstLogin(isFirstLogin)
                .adminId(admin.getAdminId())
                .adminName(admin.getAdminName())
                .adminIsSuper(admin.getAdminIsSuper())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .organizationId(organizationId)
                .organizationName(organizationName)
                .organizationSubscription(subscriptionResponse)
                .build();
    }

    @Transactional
    public AdminFindPasswordDto adminFindPassword(AdminFindPasswordRequest request) {

        Admin admin = adminRepository.findByAdminLoginIdentifier(request.getAdminLoginIdentifier())
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 아이디입니다."));

        if (admin.getFindPwdQuestionId().equals(request.getFindPwdQuestionId())) {
            if (!admin.getFindPwdAnswer().equals(request.getFindPwdAnswer())) {
                throw new UnauthorizedException("질문 혹은 답변이 일치하지 않습니다.");
            }
        } else {
            throw new UnauthorizedException("질문 혹은 답변이 일치하지 않습니다.");
        }

        return AdminFindPasswordDto.builder()
                .adminId(admin.getAdminId())
                .adminName(admin.getAdminName())
                .adminLoginIdentifier(admin.getAdminLoginIdentifier())
                .build();
    }

//    @Transactional
//    public void adminModifyPassword(AdminResetPasswordRequest request) {
//
//        Admin admin = adminRepository.findById(request.getAdminId())
//                .orElseThrow(() -> new NotFoundDataException("관리자를 찾을 수 없습니다."));
//
//        admin.updatePassword(passwordEncoder, request.getNewPassword());
//    }
}
