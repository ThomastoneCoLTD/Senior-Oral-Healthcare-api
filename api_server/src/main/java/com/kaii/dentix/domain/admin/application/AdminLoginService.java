package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminAuthDto;
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
    private final PasswordEncoder passwordEncoder;
    private final OrganizationSubscriptionRepository subscriptionRepository;

    /**
     *  관리자 로그인
     */
    @Transactional
    public AdminAuthDto.LoginResponse login(AdminAuthDto.LoginRequest request) { // DTO 교체

        // 1. 관리자 조회 (필드명 변경: adminLoginIdentifier -> loginId)
        Admin admin = adminRepository.findByAdminLoginIdentifier(request.getLoginId())
                .orElseThrow(() -> new UnauthorizedException("입력하신 정보가 일치하지 않습니다. 다시 확인해주세요."));

        // 2. 최초 로그인 여부 확인
        YnType isFirstLogin = admin.getAdminLastLoginDate() == null ? YnType.Y : YnType.N;

        // 3. 비밀번호 검증 (필드명 변경: adminPassword -> password)
        if (!passwordEncoder.matches(request.getPassword(), admin.getAdminPassword())) {
            throw new UnauthorizedException("입력하신 정보가 일치하지 않습니다. 다시 확인해주세요.");
        }

        // 4. 토큰 생성
        String accessToken = jwtTokenUtil.createToken(admin, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(admin, TokenType.RefreshToken);

        // 5. 리프레시 토큰 저장 (DB 업데이트)
        admin.updateAdminLogin(refreshToken);

        // 6. 기관 및 구독 정보 조회
        Organization org = admin.getOrganization();
        OrganizationSubscriptionResponse subscriptionResponse = null;
        Long organizationId = null;
        String organizationName = null;

        if (org != null) {
            organizationId = org.getOrganizationId();
            organizationName = org.getOrganizationName();

            OrganizationSubscription subscription = subscriptionRepository
                    .findByOrganization_OrganizationId(organizationId)
                    .orElse(null);

            if (subscription != null) {
                SubscriptionPlan plan = subscription.getSubscriptionPlan();

                // 날짜 변환 (LocalDateTime -> LocalDate)
                LocalDate startDate = toLocalDate(subscription.getSubscriptionStartDate());
                LocalDate endDate = toLocalDate(subscription.getSubscriptionEndDate());
                LocalDate resetDate = toLocalDate(subscription.getUsageResetDate());

                // 구독 정보 DTO 생성
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

        // 7. 최종 응답 DTO 생성 (AdminAuthDto.LoginResponse)
        return AdminAuthDto.LoginResponse.builder()
                .isFirstLogin(isFirstLogin)
                .adminId(admin.getAdminId())
                .adminName(admin.getAdminName())       // 필드명 변경 (adminName -> name)
                .adminIsSuper(admin.getAdminIsSuper()) // 필드명 변경 (adminIsSuper -> isSuper)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .organizationId(organizationId)
                .organizationName(organizationName)
                .organizationSubscription(subscriptionResponse) // 필요 시 DTO에 필드 추가 필요
                .build();
    }
    /**
     * 관리자 비밀번호 찾기 (본인 확인)
     */
    @Transactional(readOnly = true) // 조회만 하므로 readOnly 권장
    public AdminAuthDto.FindPasswordResponse adminFindPassword(AdminAuthDto.FindPasswordRequest request) { // DTO 교체

        // 1. 아이디로 관리자 조회
        Admin admin = adminRepository.findByAdminLoginIdentifier(request.getLoginId())
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 아이디입니다."));

        // 2. 질문/답변 검증
        if (!admin.getFindPwdQuestionId().equals(request.getQuestionId()) || // 필드명 변경 (findPwdQuestionId -> questionId)
                !admin.getFindPwdAnswer().equals(request.getAnswer())) {         // 필드명 변경 (findPwdAnswer -> answer)
            throw new UnauthorizedException("질문 혹은 답변이 일치하지 않습니다.");
        }

        // 3. 응답 DTO 생성
        return AdminAuthDto.FindPasswordResponse.builder()
                .adminId(admin.getAdminId())
                .loginId(admin.getAdminLoginIdentifier())
                .build();
    }

    // --- Helper Methods ---

    // 날짜 변환 헬퍼 (null safe)
    private LocalDate toLocalDate(java.time.LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate() : null;
    }
}
