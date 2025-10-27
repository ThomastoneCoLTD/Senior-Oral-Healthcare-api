package com.kaii.dentix.domain.admin.dto;

import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.dto.TokenDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter @SuperBuilder
@AllArgsConstructor @NoArgsConstructor
public class AdminLoginDto extends TokenDto {

    private YnType isFirstLogin;

    private Long adminId;

    private String adminName;

    private YnType adminIsSuper;
    private Long organizationId;
    private String organizationName;
}
