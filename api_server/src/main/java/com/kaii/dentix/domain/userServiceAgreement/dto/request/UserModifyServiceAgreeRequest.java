package com.kaii.dentix.domain.userServiceAgreement.dto.request;

import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;

import com.kaii.dentix.domain.type.YnType;

@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class UserModifyServiceAgreeRequest {

    @NotNull(message = "동의 항목은 필수입니다.")
    private Long serviceAgreeId;

    @NotNull(message = "동의 여부는 필수입니다.")
    private YnType isUserServiceAgree;

}
