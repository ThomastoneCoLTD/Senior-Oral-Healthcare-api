package com.kaii.dentix.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Getter
    @NoArgsConstructor
    public static class IntegratedLoginRequest {
        @NotBlank(message = "사용자 타입은 필수입니다. (admin/user)")
        private String userType;

        @NotBlank(message = "아이디를 입력해주세요.")
        private String loginId;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        private String password;
    }
}