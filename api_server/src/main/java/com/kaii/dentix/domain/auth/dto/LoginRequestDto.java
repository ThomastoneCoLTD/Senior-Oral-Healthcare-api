package com.kaii.dentix.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequestDto {
    @NotBlank
    private String userType; // "admin" 또는 "user"

    @NotBlank
    private String loginId;

    @NotBlank
    private String password;
}
