package com.kaii.dentix.domain.superAdmin.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminUserStatisticResponse {

    private Long organizationId;
    private String organizationName;
    private Long totalUsers;
    private Long maleUsers;
    private Long femaleUsers;
    private Long newUsers; // 최근 30일 내 신규가입자
}