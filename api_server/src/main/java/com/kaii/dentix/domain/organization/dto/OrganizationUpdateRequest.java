package com.kaii.dentix.domain.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class OrganizationUpdateRequest {

    @NotBlank(message = "기관명은 필수입니다.")
    private String organizationName;

    @NotBlank(message = "기관 연락처는 필수입니다.")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "전화번호는 숫자만 입력 가능합니다.")
    private String organizationPhoneNumber;

    @Email(message = "올바른 이메일 형식이어야 합니다.")
    private String organizationEmail;
}