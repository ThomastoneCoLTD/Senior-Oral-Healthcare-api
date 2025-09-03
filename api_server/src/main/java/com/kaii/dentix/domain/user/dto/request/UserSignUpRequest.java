package com.kaii.dentix.domain.user.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.global.config.PasswordSerializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class UserSignUpRequest {

    @NotNull(message = "서비스 동의 체크는 필수입니다.")
    @Valid
    private List<Long>  userServiceAgreementRequest;

    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 4, max = 12, message = "아이디는 최소 4자부터 최대 12자입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 숫자나 영문만 사용 가능해요.")
    private String userLoginIdentifier;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 100, message = "닉네임은 최소 2자 이상 입력해야 됩니다.")
    @Pattern(regexp = "^[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z\\s]+$", message = "닉네임은 한글이나 영문으로만 입력해 주세요.")
    private String userName;

    private GenderType userGender;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 최소 8자부터 최대 20자입니다.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[!@#$%^&*])[a-zA-Z!@#$%^&*0-9]+$", message = "비밀번호는 영문과 특수문자가 필수입니다.")
    @JsonSerialize(using = PasswordSerializer.class)
    private String userPassword;

    @NotNull(message = "질문 선택 필수입니다.")
    private Long findPwdQuestionId;

    @NotBlank(message = "답변은 필수입니다.")
    private String findPwdAnswer;

    private Long userId;


}
