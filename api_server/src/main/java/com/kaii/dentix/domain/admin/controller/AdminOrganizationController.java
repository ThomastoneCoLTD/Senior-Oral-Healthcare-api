package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminOrganizationService;
import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.application.AdminUserService;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminListDto;
import com.kaii.dentix.domain.admin.dto.AdminOrganizationUsageResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.global.common.dto.PageAndSizeRequest;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/organization")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminOrganizationController {

    private final AdminOrganizationService adminOrganizationService;
    private final AdminService adminService;
//private final AdminOrganizationService adminOrganizationService;

    @GetMapping("/organization")
    public ResponseEntity<?> getMyOrganization(HttpServletRequest request) {
        return ResponseEntity.ok(adminOrganizationService.getMyOrganization(request));
    }
    @GetMapping("/usage")
    public ResponseEntity<?> getAllOrganizationUsage(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);
        List<AdminOrganizationUsageResponse> result = adminOrganizationService.getAllOrganizationUsage(request, admin);
        return ResponseEntity.ok(result);
    }
}