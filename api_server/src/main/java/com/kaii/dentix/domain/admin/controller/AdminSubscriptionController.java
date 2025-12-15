package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.organization.application.OrganizationSubscriptionService;
import com.kaii.dentix.domain.organization.dto.OrganizationSubscriptionChangeRequest;
import com.kaii.dentix.domain.organization.dto.OrganizationSubscriptionResponse;
import com.kaii.dentix.domain.organizationSubscriptionHistory.application.OrganizationSubscriptionHistoryService;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dto.OrganizationSubscriptionHistoryResponse;
import com.kaii.dentix.domain.subscription.application.SubscriptionInfoService;
import com.kaii.dentix.domain.subscription.application.SubscriptionService;
import com.kaii.dentix.domain.subscription.dto.SubscriptionHistoryResponse;
import com.kaii.dentix.domain.subscription.dto.SubscriptionInfoResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Slf4j

@RestController
@RequestMapping("/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {
    private final SubscriptionInfoService subscriptionInfoService;
    private final SubscriptionService subscriptionService;
    private final AdminRepository adminRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final OrganizationSubscriptionService organizationSubscriptionService;
    private final AdminService adminService;
    private final OrganizationSubscriptionHistoryService organizationSubscriptionHistoryService;


    @GetMapping("/all")
    public ResponseEntity<List<SubscriptionInfoResponse>> getAllPlans() {
        List<SubscriptionInfoResponse> plans = subscriptionInfoService.getAllPlans();
        return ResponseEntity.ok(plans);
    }

    /** 일반관리자 - 본인 기관 구독 정보 조회 */
    @GetMapping("/my")
    public ResponseEntity<OrganizationSubscriptionResponse> getMySubscription() {
        Long adminId = jwtTokenUtil.getCurrentAdminId();

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 계정을 찾을 수 없습니다."));

        OrganizationSubscriptionResponse response =
                organizationSubscriptionService.getMySubscription(adminId);

        return ResponseEntity.ok(response);
    }

    /** 일반관리자 - 본인 기관 구독 정보 수정 */
    @PutMapping("/my")
    public ResponseEntity<SuccessResponse> updateMySubscription(
            HttpServletRequest request,
            @RequestBody OrganizationSubscriptionChangeRequest dto
    ) {
        Admin admin = adminService.getTokenAdmin(request);   //Admin 인증
        SuccessResponse response = subscriptionService.updateMyOrganizationSubscription(admin, dto);
        return ResponseEntity.ok(response);
    }

    /** 일반관리자 - 본인 기관의 구독 이력 조회 */
    @GetMapping("/history")
    public ResponseEntity<List<OrganizationSubscriptionHistoryResponse>> getMySubscriptionHistory() {
        Long adminId = jwtTokenUtil.getCurrentAdminId();

        List<OrganizationSubscriptionHistoryResponse> histories =
                organizationSubscriptionHistoryService.getMySubscriptionHistory(adminId);

        return ResponseEntity.ok(histories);
    }

    /** ???기관별 구독 이력 조회 */
    @GetMapping("/{organizationId}/subscription-history")
    public ResponseEntity<List<SubscriptionHistoryResponse>> getSubscriptionHistory(
            @PathVariable Long organizationId
    ) {
        List<SubscriptionHistoryResponse> historyList =
                organizationSubscriptionHistoryService.getSubscriptionHistoryByOrganization(organizationId);
        return ResponseEntity.ok(historyList);
    }
}
