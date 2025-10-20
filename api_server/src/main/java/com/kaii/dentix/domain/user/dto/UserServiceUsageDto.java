package com.kaii.dentix.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserServiceUsageDto {
    private Long userId;
    private String userName;
    private String userPhoneNumber;
    private String organizationName;
    private String serviceName;
    private Long successCount;
}