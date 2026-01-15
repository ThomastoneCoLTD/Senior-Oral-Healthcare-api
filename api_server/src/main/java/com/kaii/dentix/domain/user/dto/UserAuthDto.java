package com.kaii.dentix.domain.user.dto;

import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.ServiceType;
import com.kaii.dentix.domain.type.YnType;
import lombok.*;

import java.util.List;

public class UserAuthDto {

    // =================================================================
    // 1. 로그인 (Login)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        private String loginId;
        private String password;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class LoginResponse {
        private Long userId;
        private String userName;
        private String accessToken;
        private String refreshToken;

        // 메인 서비스 정보 (기존 로직 유지)
        private Long serviceId;
        private String name;

        // 연동된 서비스 목록
        private List<AppServiceInfo> services;

        // 기관 정보
        private Long organizationId;
        private String organizationName;
        private String organizationPlanName;
        private Boolean organizationCustomSurveyEnabled;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class AppServiceInfo {
        private Long serviceId;
        private String name;
        private ServiceType serviceType;
    }

    // =================================================================
    // 2. 회원 인증 (Verify)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class VerifyRequest {
        private String userName;
        private String userPhoneNumber;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class VerifyResponse {
        private Long userId; // 이미 가입된 경우 ID 반환, 아니면 null
    }

    // =================================================================
    // 3. 회원 가입 (SignUp)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class SignUpRequest {
        private String userLoginIdentifier;
        private String userName;
        private GenderType userGender;
        private String userPassword;
        private Long findPwdQuestionId;
        private String findPwdAnswer;
        private String userPhoneNumber;
        private Long organizationId;
        private List<Long> appServiceIds;
        private List<Long> userServiceAgreementRequest; // 약관 동의 ID 목록
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class SignUpResponse {
        private Long userId;
        private String accessToken;
        private String refreshToken;
        private String userLoginIdentifier;
        private String userName;
        private GenderType userGender;
        private Long organizationId;
        private String organizationName;
    }

    // =================================================================
    // 4. 비밀번호 찾기 (FindPassword)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class FindPasswordRequest {
        private String userLoginIdentifier;
        private Long findPwdQuestionId;
        private String findPwdAnswer;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class FindPasswordResponse {
        private Long userId;
        private String userName;
        private String userLoginIdentifier;
    }
}