package com.kaii.dentix.domain.user.dto;

import com.kaii.dentix.domain.type.GenderType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter @SuperBuilder
@AllArgsConstructor
public class UserSignUpDto extends TokenDto{

    private Long userId;

    private String userLoginIdentifier;

    private String userName;

    private GenderType userGender;

    private Long organizationId;
    private String organizationName;

}
