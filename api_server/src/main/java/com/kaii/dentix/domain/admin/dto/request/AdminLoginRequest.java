package com.kaii.dentix.domain.admin.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kaii.dentix.global.config.PasswordSerializer;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Builder
@Setter
@AllArgsConstructor @NoArgsConstructor
public class AdminLoginRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    private String adminLoginIdentifier;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @JsonSerialize(using = PasswordSerializer.class)
    private String adminPassword;
}
