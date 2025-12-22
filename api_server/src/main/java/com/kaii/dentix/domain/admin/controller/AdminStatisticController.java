package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.application.AdminStatisticService;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminStatisticsOrgUserResponse;
import com.kaii.dentix.domain.admin.dto.request.AdminStatisticRequest;
import com.kaii.dentix.domain.admin.dto.statistic.AdminUserStatisticResponse;
import com.kaii.dentix.domain.admin.dto.superAdmin.SuperAdminUserStatisticResponse;
import com.kaii.dentix.domain.subscription.application.SubscriptionInfoService;
import com.kaii.dentix.domain.subscription.dto.SubscriptionInfoResponse;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/statistic")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminStatisticController {

    private final AdminStatisticService adminStatisticService;

    @GetMapping("/org/users")
    public ResponseEntity<AdminStatisticsOrgUserResponse> getOrganizationUserStatistics(
            HttpServletRequest httpRequest
    ) {
        AdminStatisticsOrgUserResponse response = adminStatisticService.getOrganizationUserStatistics(httpRequest);
        return ResponseEntity.ok(response);
    }

}
