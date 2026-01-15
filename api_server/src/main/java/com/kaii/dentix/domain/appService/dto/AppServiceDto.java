package com.kaii.dentix.domain.appService.dto;

import com.kaii.dentix.domain.appService.domain.AppService;
import com.kaii.dentix.domain.type.ServiceType;
import com.kaii.dentix.domain.type.YnType;
import lombok.*;

public class AppServiceDto {

    // =================================================================
    // 1. 사용자별 앱 서비스 연동 현황 응답 (Response)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class UsageStatus {
        private Long appServiceId;       // 서비스 ID (예: 1)
        private String serviceName;      // 서비스 이름 (예: 카카오, 네이버)
        private ServiceType serviceType; // 서비스 타입 (ENUM)
        private YnType isConnected;      // 연동 여부 (Y/N)
        private String connectedDate;    // 연동된 날짜 (예: 2023.01.01)
    }

    // =================================================================
    // 2. 앱 서비스 연동 요청 (Request)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ConnectRequest {
        private Long appServiceId; // 연동할 서비스 ID
        // private String authCode; // 추후 인증 코드가 필요하면 추가
    }



}