package com.kaii.dentix.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminResetPasswordRequest {

    @NotNull(message = "관리자 ID는 필수입니다.")
    private Long adminId;

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    private String newPassword;
}