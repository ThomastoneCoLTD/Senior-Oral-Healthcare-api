package com.kaii.dentix.domain.user.dto;

import com.kaii.dentix.domain.type.ServiceType;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter @SuperBuilder
@AllArgsConstructor
public class UserLoginDto extends TokenDto{

    private Long userId;
    private String userName;
    private Long serviceId; // 대표 서비스
    private String name;


    //사용자와 연결된 서비스 전체 목록
    private List<AppServiceInfo> services;
    private Long organizationId;
    private String organizationName;

    //기관 구독정보 추가
    private String organizationPlanName;              // SMALL, GROWTH, MID 등
    private Boolean organizationCustomSurveyEnabled;
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AppServiceInfo {
        private Long serviceId;
        private String name;
        private ServiceType serviceType;
    }
}
