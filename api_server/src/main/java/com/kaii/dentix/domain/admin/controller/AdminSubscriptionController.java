package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.organization.application.OrganizationSubscriptionService;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
import com.kaii.dentix.domain.organizationSubscriptionHistory.application.OrganizationSubscriptionHistoryService;
import com.kaii.dentix.domain.subscription.application.SubscriptionService;
import com.kaii.dentix.domain.subscription.dto.SubscriptionDto;
import com.kaii.dentix.global.common.response.DataResponse;
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
    private final AdminService adminService;
    private final JwtTokenUtil jwtTokenUtil;
    private final AdminRepository adminRepository;
    private final SubscriptionService subscriptionService;
//    private final SubscriptionInfoService subscriptionInfoService;
    private final OrganizationSubscriptionService organizationSubscriptionService;
    private final OrganizationSubscriptionHistoryService organizationSubscriptionHistoryService;

    @GetMapping("/all")
    public ResponseEntity<DataResponse<List<SubscriptionDto.PlanResponse>>> getAllPlans() {
        return ResponseEntity.ok(
                new DataResponse<>(200, "전체 구독플랜 조회 성공", subscriptionService.getAllPlans())
        );
    }

    /** 일반관리자 - 본인 기관 구독 정보 조회 */
    @GetMapping("/my")
    public ResponseEntity<OrganizationDto.SubscriptionResponse> getMySubscription() {
        Long adminId = jwtTokenUtil.getCurrentAdminId();

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 계정을 찾을 수 없습니다."));

        OrganizationDto.SubscriptionResponse response =
                organizationSubscriptionService.getMySubscription(adminId);

        return ResponseEntity.ok(response);
    }

    /** 일반관리자 - 본인 기관 구독 정보 수정 */
    @PutMapping("/my")
    public ResponseEntity<SuccessResponse> updateMySubscription(
            HttpServletRequest request,
            @RequestBody OrganizationDto.SubscriptionChangeRequest dto
    ) {
        Admin admin = adminService.getTokenAdmin(request);
        SuccessResponse response = subscriptionService.updateMyOrganizationSubscription(admin, dto);
        return ResponseEntity.ok(response);
    }

    /** 일반관리자 - 본인 기관의 구독 이력 조회 */
    @GetMapping("/history")
    public ResponseEntity<List<OrganizationDto.SubscriptionHistoryResponse>> getMySubscriptionHistory() {
        Long adminId = jwtTokenUtil.getCurrentAdminId();

        List<OrganizationDto.SubscriptionHistoryResponse> histories =
                organizationSubscriptionHistoryService.getMySubscriptionHistory(adminId);

        return ResponseEntity.ok(histories);
    }

    /** ???기관별 구독 이력 조회 */
    @GetMapping("/{organizationId}/subscription-history")
    public ResponseEntity<List<OrganizationDto.SubscriptionHistoryResponse>> getSubscriptionHistory(
            @PathVariable Long organizationId
    ) {
        List<OrganizationDto.SubscriptionHistoryResponse> historyList =
                organizationSubscriptionHistoryService.getSubscriptionHistoryByOrganization(organizationId);

        return ResponseEntity.ok(historyList);
    }
}
