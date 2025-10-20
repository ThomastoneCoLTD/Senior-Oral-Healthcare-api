package com.kaii.dentix.domain.user.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserServiceChangeDto {
    private String userName;
    private List<ServiceInfo> services;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceInfo {
        private Long serviceId;
        private String serviceName;
        private String serviceType;
    }
}