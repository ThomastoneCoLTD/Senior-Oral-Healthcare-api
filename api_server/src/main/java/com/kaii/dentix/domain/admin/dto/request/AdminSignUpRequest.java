package com.kaii.dentix.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class AdminSignUpRequest {

    @NotBlank(message = "이름은 필수입니다.")
    @Size(min = 2, max = 100, message = "이름은 최소 2자 이상 입력해야 됩니다.")
    @Pattern(regexp = "^[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z\\s]+$", message = "이름은 한글이나 영문으로만 입력해 주세요.")
    private String adminName;

    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 4, max = 12, message = "아이디는 최소 4자부터 최대 12자입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 숫자나 영문만 사용 가능해요.")
    private String adminLoginIdentifier;

    @NotBlank(message = "연락처는 필수입니다.")
    @Size(min = 10, max = 11, message = "연락처는 최소 10자부터 최대 11자입니다.")
    @Pattern(regexp = "^[0-9]+$", message = "연락처는 숫자만 입력해 주세요.")
    private String adminPhoneNumber;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 최소 8자 이상, 최대 20자 이하입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-])[A-Za-z\\d!@#$%^&*()_+=\\-]{8,20}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다."
    )
    private String adminPassword;

    @NotNull(message = "비밀번호 찾기 질문은 필수입니다.")
    private Long findPwdQuestionId;

    @NotBlank(message = "비밀번호 찾기 답변은 필수입니다.")
    @Size(min = 2, max = 100, message = "답변은 최소 2자 이상 입력해야 됩니다.")
    private String findPwdAnswer;

}
