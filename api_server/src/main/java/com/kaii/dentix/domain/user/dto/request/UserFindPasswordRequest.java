package com.kaii.dentix.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class UserFindPasswordRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 4, max = 12, message = "아이디는 최소 4자부터 최대 12자입니다.")
    private String userLoginIdentifier;

    @NotNull(message = "질문 선택은 필수입니다.")
    private Long findPwdQuestionId;

    @NotBlank(message = "답변은 필수입니다.")
    private String findPwdAnswer;
}
