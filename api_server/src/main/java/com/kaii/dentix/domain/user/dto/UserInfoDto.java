package com.kaii.dentix.domain.user.dto;

import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.userServiceAgreement.dto.UserServiceAgreeList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter @Builder
@AllArgsConstructor
public class UserInfoDto {

    private String userName;

    private String userLoginIdentifier;

    private String patientPhoneNumber;

    private List<UserServiceAgreeList> userServiceAgreeLists;

    private GenderType userGender;
    // ✅ 연결된 서비스 목록
    private List<ServiceInfo> services;

    /**
     * 사용자가 연결된 AppService 요약 정보
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServiceInfo {
        private Long serviceId;      // 서비스 ID
        private String name;         // 서비스 이름 (예: 플라그 검출, 치주염 분석 등)
        private String serviceType;  // 서비스 타입 (ENUM 문자열)
    }

}
