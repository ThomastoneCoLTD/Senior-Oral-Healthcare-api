package com.kaii.dentix.domain.user.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.ServiceType;
import com.kaii.dentix.domain.user.domain.UserDaeguIdentityStatus;
import com.kaii.dentix.global.config.PasswordSerializer;
import com.kaii.dentix.global.config.SensitiveJsonNodeSerializer;
import com.kaii.dentix.global.config.SensitiveStringSerializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

public class UserDto {

    // =================================================================
    // 1. 공통 / 토큰 (Token)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class AccessTokenResponse {
        private String accessToken;
    }

    // =================================================================
    // 2. 인증 (Auth: Login, Verify)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "아이디는 필수입니다.")
        private String userLoginIdentifier;

        @NotBlank(message = "비밀번호는 필수입니다.")
        @JsonSerialize(using = PasswordSerializer.class)
        private String userPassword;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class DadaeguLoginRequest {
        @NotNull(message = "다대구 인증 결과는 필수입니다.")
        @JsonSerialize(using = SensitiveJsonNodeSerializer.class)
        private JsonNode encryptedData;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class DadaeguLoginConfigResponse {
        private boolean enabled;
        private String siteId;
        private String requiredVc;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class LoginResponse {
        private Long userId;
        private String userName;
        private String accessToken;
        private String refreshToken;

        // 대표 서비스
        private Long serviceId;
        private String name;

        // 전체 서비스 목록
        private List<ServiceInfo> services;

        // 기관 정보
        private Long organizationId;
        private String organizationName;
        private String organizationPlanName;
        private Boolean organizationCustomSurveyEnabled;
        private boolean oralAnalysisServiceEnabled;
        private String daeguDid;
        private UserDaeguIdentityStatus daeguDidStatus;

        // 다대구 최초 로그인 시에만 사용되는 추가가입 정보
        private Boolean dadaeguOnboardingRequired;
        private String dadaeguOnboardingToken;
        private Long dadaeguOnboardingExpiresInSeconds;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class DadaeguSignUpRequest {
        @NotBlank(message = "다대구 가입 진행 토큰은 필수입니다.")
        @JsonSerialize(using = SensitiveStringSerializer.class)
        private String onboardingToken;

        @NotBlank(message = "기관 선택은 필수입니다.")
        @Pattern(regexp = "^(대구1|대구2|대구3)$", message = "기관은 대구1, 대구2, 대구3 중에서 선택해 주세요.")
        private String realOrganization;

        @NotNull(message = "필수 약관 동의는 필수입니다.")
        private List<Long> userServiceAgreementRequest;

        @NotNull(message = "구강분석 서비스 선택 여부는 필수입니다.")
        private Boolean oralAnalysisServiceEnabled;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class VerifyRequest {
        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        @Size(max = 20, message = "Phone number must be 20 characters or less.")
        @Pattern(regexp = "^[0-9\\-\\s()]+$", message = "Phone number can contain only numbers, hyphens, spaces, and parentheses.")
        private String userPhoneNumber;

        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 100, message = "이름 최소 2자 이상 입력해야 됩니다.")
        @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Name can contain only letters and spaces.")
        private String userName;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class VerifyResponse {
        private Long userId; // null이면 신규 가입 가능
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class FindIdRequest {
        @NotBlank(message = "이름은 필수입니다.")
        private String userName;

        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        @Size(max = 20, message = "Phone number must be 20 characters or less.")
        @Pattern(regexp = "^[0-9\\-\\s()]+$", message = "Phone number can contain only numbers, hyphens, spaces, and parentheses.")
        private String userPhoneNumber;

        @NotBlank(message = "생년월일은 필수입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 YYYY-MM-DD 형식으로 입력해 주세요.")
        private String userBirthDate;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class FindIdResponse {
        private String userLoginIdentifier;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class FindPasswordRequest {
        @NotBlank(message = "아이디는 필수입니다.")
        private String userLoginIdentifier;

        @NotNull(message = "비밀번호 찾기 질문 선택은 필수입니다.")
        private Long findPwdQuestionId;

        @NotBlank(message = "비밀번호 찾기 답변은 필수입니다.")
        private String findPwdAnswer;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class FindPasswordResponse {
        private String resetToken;
        private long expiresInSeconds;
        private String userLoginIdentifier;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ModifyPasswordRequest {
        @NotBlank(message = "비밀번호 재설정 토큰은 필수입니다.")
        private String resetToken;

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 최소 8자부터 최대 20자입니다.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[!@#$%^&*])[a-zA-Z!@#$%^&*0-9]+$", message = "비밀번호는 영문과 특수문자가 필수입니다.")
        @JsonSerialize(using = PasswordSerializer.class)
        private String userPassword;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ServiceInfo {
        private Long serviceId;
        private String name;
        private ServiceType serviceType;
    }

    public static List<ServiceInfo> defaultServiceInfo() {
        return List.of(
                ServiceInfo.builder()
                        .serviceId(1L)
                        .name("PLAQUE")
                        .serviceType(ServiceType.PLAQUE_DETECTION)
                        .build(),
                ServiceInfo.builder()
                        .serviceId(2L)
                        .name("PERIODONTAL")
                        .serviceType(ServiceType.PERIODONTAL_DETECTION)
                        .build()
        );
    }

    public static List<ServiceInfo> serviceInfo(boolean oralAnalysisServiceEnabled) {
        return oralAnalysisServiceEnabled ? defaultServiceInfo() : List.of();
    }

    // =================================================================
    // 3. 회원가입 (SignUp)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class SignUpRequest {
        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min = 4, max = 12, message = "아이디는 최소 4자부터 최대 12자입니다.")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 숫자나 영문만 사용 가능해요.")
        private String userLoginIdentifier;

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 최소 8자부터 최대 20자입니다.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[!@#$%^&*])[a-zA-Z!@#$%^&*0-9]+$", message = "비밀번호는 영문과 특수문자가 필수입니다.")
        @JsonSerialize(using = PasswordSerializer.class)
        private String userPassword;

        @NotBlank(message = "닉네임은 필수입니다.")
        private String userName;

        @NotNull(message = "성별 선택은 필수입니다.")
        private GenderType userGender;
        private String userPhoneNumber;

        @NotBlank(message = "생년월일은 필수입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 YYYY-MM-DD 형식으로 입력해 주세요.")
        private String userBirthDate;

        @NotBlank(message = "기관 선택은 필수입니다.")
        @Pattern(regexp = "^(대구1|대구2|대구3)$", message = "기관은 대구1, 대구2, 대구3 중에서 선택해 주세요.")
        private String realOrganization;

        @NotNull(message = "비밀번호 찾기 질문 선택은 필수입니다.")
        private Long findPwdQuestionId;
        @NotBlank(message = "비밀번호 찾기 답변은 필수입니다.")
        private String findPwdAnswer;
        private Long organizationId;

        @NotNull(message = "서비스 동의 체크는 필수입니다.")
        private List<Long> userServiceAgreementRequest;

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
        private String realOrganization;
        private String daeguDid;
        private UserDaeguIdentityStatus daeguDidStatus;
        private String walletAddress;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class DidSignUpRequest {
        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min = 4, max = 12, message = "아이디는 최소 4자부터 최대 12자입니다.")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 숫자와 영문만 사용할 수 있습니다.")
        private String userLoginIdentifier;

        @NotBlank(message = "이름은 필수입니다.")
        private String userName;

        @NotNull(message = "성별 선택은 필수입니다.")
        private GenderType userGender;

        @NotBlank(message = "전화번호는 필수입니다.")
        @Size(max = 20, message = "Phone number must be 20 characters or less.")
        @Pattern(regexp = "^[0-9\\-\\s()]+$", message = "Phone number can contain only numbers, hyphens, spaces, and parentheses.")
        private String userPhoneNumber;

        @NotBlank(message = "생년월일은 필수입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 YYYY-MM-DD 형식으로 입력해 주세요.")
        private String userBirthDate;

        @NotBlank(message = "기관 선택은 필수입니다.")
        @Pattern(regexp = "^(대구1|대구2|대구3)$", message = "기관은 대구1, 대구2, 대구3 중에서 선택해 주세요.")
        private String realOrganization;

        @NotNull(message = "서비스 이용 동의는 필수입니다.")
        private List<Long> userServiceAgreementRequest;

    }

    // =================================================================
    // 4. 회원정보 조회/수정 (Info, Modify)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class InfoResponse {
        private String userName;
        private String userLoginIdentifier;
        private GenderType userGender;
        private String realOrganization;
        private List<ServiceInfo> services;
        private boolean oralAnalysisServiceEnabled;
        private String daeguDid;
        private UserDaeguIdentityStatus daeguDidStatus;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ModifyInfoRequest {
        @NotBlank(message = "닉네임은 필수입니다.")
        private String userName;
        private GenderType userGender;
        private Boolean oralAnalysisServiceEnabled;
    }

    // =================================================================
    // 5. QnA 수정 및 서비스 변경
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ModifyQnARequest {
        @NotNull private Long findPwdQuestionId;
        @NotBlank private String findPwdAnswer;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ModifyQnAResponse {
        private Long findPwdQuestionId;
        private String findPwdAnswer;
    }


    // =================================================================
    // 7. 서비스 사용 통계 (Usage)
    // =================================================================
    @Getter
    @NoArgsConstructor
    public class ServiceUsageResponse {

        private Long userId;
        private String userName;
        private String userPhoneNumber;
        private String organizationName;
        private String serviceName;
        private Long successCount;

        public ServiceUsageResponse(Long userId, String userName, String userPhoneNumber,
                                    String organizationName, String serviceName, Long successCount) {
            this.userId = userId;
            this.userName = userName;
            this.userPhoneNumber = userPhoneNumber;
            this.organizationName = organizationName;
            this.serviceName = serviceName;
            this.successCount = successCount;
        }
    }
}
