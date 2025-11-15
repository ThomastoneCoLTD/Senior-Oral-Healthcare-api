package com.kaii.dentix.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminFindPasswordRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    private String adminLoginIdentifier;

    @NotNull(message = "질문 ID는 필수입니다.")
    private Long findPwdQuestionId;

    @NotBlank(message = "질문 답변은 필수입니다.")
    private String findPwdAnswer;
}