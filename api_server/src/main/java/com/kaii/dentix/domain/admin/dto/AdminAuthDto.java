package com.kaii.dentix.domain.admin.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.dto.TokenDto;
import com.kaii.dentix.global.config.PasswordSerializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

public class AdminAuthDto {

    // =================================================================
    // 1. 로그인 (Login)
    // =================================================================
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "아이디는 필수입니다.")
        private String loginId; // adminLoginIdentifier -> loginId (단축)

        @NotBlank(message = "비밀번호는 필수입니다.")
        @JsonSerialize(using = PasswordSerializer.class)
        private String password; // adminPassword -> password (문맥상 명확하므로 단축)
    }

    @Getter @SuperBuilder
    @NoArgsConstructor @AllArgsConstructor
    public static class LoginResponse extends TokenDto {
        private YnType isFirstLogin;
        private Long adminId;
        private String adminName;
        private YnType adminIsSuper;

        // 기관 정보
        private Long organizationId;
        private String organizationName;
        private OrganizationDto.SubscriptionResponse organizationSubscription;

        //정적 팩토리 메서드: Service 로직을 깔끔하게 만듦
        public static LoginResponse from(Admin admin, String accessToken, String refreshToken, OrganizationDto.SubscriptionResponse subResponse) {
            Organization org = admin.getOrganization();

            return LoginResponse.builder()
                    .isFirstLogin(admin.isFirstLogin() ? YnType.Y : YnType.N) // Entity 메서드 활용
                    .adminId(admin.getAdminId())
                    .adminName(admin.getAdminName())
                    .adminIsSuper(admin.getAdminIsSuper())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .organizationId(org != null ? org.getOrganizationId() : null)
                    .organizationName(org != null ? org.getOrganizationName() : null)
                    .organizationSubscription(subResponse)
                    .build();
        }
    }

    // 자동 로그인 응답
    @Getter @SuperBuilder
    @NoArgsConstructor @AllArgsConstructor
    public static class AutoLoginResponse extends TokenDto {
        private Long adminId;
        private String adminName;
        private YnType adminIsSuper;

        public static AutoLoginResponse from(Admin admin, String accessToken, String refreshToken) {
            return AutoLoginResponse.builder()
                    .adminId(admin.getAdminId())      // 괄호 () 필수
                    .adminName(admin.getAdminName())  // 괄호 () 필수
                    .adminIsSuper(admin.getAdminIsSuper())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }
    }

    // =================================================================
    // 2. 회원가입 (SignUp)
    // =================================================================
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class SignUpRequest {
        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 100)
        @Pattern(regexp = "^[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z\\s]+$")
        private String name;

        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min = 4, max = 12)
        @Pattern(regexp = "^[a-zA-Z0-9]+$")
        private String loginId;

        @NotBlank
        @Pattern(regexp = "^[0-9]+$")
        private String phoneNumber;

        @NotBlank
        @Size(min = 8, max = 20)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-]).{8,20}$")
        private String password;

        @NotNull
        private Long findPwdQuestionId;

        @NotBlank
        private String findPwdAnswer;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class SignUpResponse {
        private Long adminId;
        private String adminName;

        public static SignUpResponse of(Admin admin) {
            return SignUpResponse.builder()
                    .adminId(admin.getAdminId())
                    .adminName(admin.getAdminName())
                    .build();
        }
    }

    // =================================================================
    // 3. 비밀번호 찾기/변경 (Password)
    // =================================================================
    @Getter @Setter
    public static class FindPasswordRequest {
        @NotBlank private String loginId;
        @NotNull private Long questionId;
        @NotBlank private String answer;
    }

    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class FindPasswordResponse {
        private Long adminId;
        private String adminName;
        private String loginId;

        public static FindPasswordResponse of(Admin admin) {
            return FindPasswordResponse.builder()
                    .adminId(admin.getAdminId())
                    .adminName(admin.getAdminName())
                    .loginId(admin.getAdminLoginIdentifier())
                    .build();
        }
    }

    @Getter @Setter
    public static class ModifyPasswordRequest {
        @NotBlank
        @Size(min = 8, max = 20)
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[!@#$%^&*])[a-zA-Z!@#$%^&*0-9]+$")
        @JsonSerialize(using = PasswordSerializer.class)
        private String password;
    }
}