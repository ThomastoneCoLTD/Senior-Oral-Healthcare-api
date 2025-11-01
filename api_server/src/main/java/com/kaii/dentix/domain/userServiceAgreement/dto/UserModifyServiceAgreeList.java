package com.kaii.dentix.domain.userServiceAgreement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kaii.dentix.domain.type.YnType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class UserModifyServiceAgreeList {

    @JsonProperty(value = "serviceAgreeId")
    private Long serviceAgreeId;

    private YnType isUserServiceAgree;
}