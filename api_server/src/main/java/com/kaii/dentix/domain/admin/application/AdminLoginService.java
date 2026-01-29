package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminAuthDto;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
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
     * 관리자 로그인
     */
    @Transactional
    public AdminAuthDto.LoginResponse login(AdminAuthDto.LoginRequest request) {

        // 1. 관리자 조회
        Admin admin = adminRepository.findByAdminLoginIdentifier(request.getLoginId())
                .orElseThrow(() -> new UnauthorizedException("입력하신 정보가 일치하지 않습니다. 다시 확인해주세요."));

        // 2. 최초 로그인 여부 확인
        YnType isFirstLogin = admin.getAdminLastLoginDate() == null ? YnType.Y : YnType.N;

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), admin.getAdminPassword())) {
            throw new UnauthorizedException("입력하신 정보가 일치하지 않습니다. 다시 확인해주세요.");
        }

        // 4. 토큰 생성
        String accessToken = jwtTokenUtil.createToken(admin, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(admin, TokenType.RefreshToken);

        // 5. 리프레시 토큰 저장
        admin.updateAdminLogin(refreshToken);

        // 6. 기관 및 구독 정보 조회
        Organization org = admin.getOrganization();

        // [수정 1] 변수 타입 변경
        OrganizationDto.SubscriptionResponse subscriptionResponse = null;
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

                // 날짜 변환 (toLocalDate 메서드가 내부에 있다고 가정)
                LocalDate startDate = toLocalDate(subscription.getSubscriptionStartDate());
                LocalDate endDate = toLocalDate(subscription.getSubscriptionEndDate());
                LocalDate resetDate = toLocalDate(subscription.getUsageResetDate());

                // [수정 2] 클래스명 변경 (OrganizationDto 내부 클래스 사용)
                subscriptionResponse = OrganizationDto.SubscriptionResponse.fromEntity(
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

        // 7. 최종 응답 DTO 생성
        return AdminAuthDto.LoginResponse.builder()
                .isFirstLogin(isFirstLogin)
                .adminId(admin.getAdminId())
                .adminName(admin.getAdminName())
                .adminIsSuper(admin.getAdminIsSuper())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .organizationId(organizationId)
                .organizationName(organizationName)
                .organizationSubscription(subscriptionResponse) // 수정된 객체 주입
                .build();
    }
    /**
     * 관리자 비밀번호 찾기 (본인 확인)
     */
    @Transactional(readOnly = true)
    public AdminAuthDto.FindPasswordResponse adminFindPassword(AdminAuthDto.FindPasswordRequest request) { // DTO 교체

        // 1. 아이디로 관리자 조회
        Admin admin = adminRepository.findByAdminLoginIdentifier(request.getLoginId())
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 아이디입니다."));

        // 2. 질문/답변 검증
        if (!admin.getFindPwdQuestionId().equals(request.getQuestionId()) ||
                !admin.getFindPwdAnswer().equals(request.getAnswer())) {
            throw new UnauthorizedException("질문 혹은 답변이 일치하지 않습니다.");
        }

        // 3. 응답 DTO 생성
        return AdminAuthDto.FindPasswordResponse.builder()
                .adminId(admin.getAdminId())
                .loginId(admin.getAdminLoginIdentifier())
                .build();
    }

    // 날짜 변환 헬퍼 (null safe)
    private LocalDate toLocalDate(java.time.LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate() : null;
    }
}
