package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.application.OrganizationSubscriptionService;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationSubscriptionChangeRequest;
import com.kaii.dentix.domain.organization.dto.OrganizationSubscriptionResponse;
import com.kaii.dentix.domain.subscription.application.SubscriptionInfoService;
import com.kaii.dentix.domain.subscription.application.SubscriptionService;
import com.kaii.dentix.domain.subscription.dto.SubscriptionInfoResponse;
import com.kaii.dentix.domain.subscription.dto.SubscriptionPlanUpdateRequest;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import io.jsonwebtoken.Jwt;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {
    private final SubscriptionInfoService subscriptionInfoService;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final OrganizationService organizationService;
    private final AdminRepository adminRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final OrganizationSubscriptionService organizationSubscriptionService;

    @GetMapping("/all")
    public ResponseEntity<List<SubscriptionInfoResponse>> getAllPlans() {
        List<SubscriptionInfoResponse> plans = subscriptionInfoService.getAllPlans();
        return ResponseEntity.ok(plans);
    }

    /**
     * ✅ 본인 기관 구독 정보 조회
     */
    @GetMapping("/my")
    public ResponseEntity<OrganizationSubscriptionResponse> getMySubscription() {
        Long adminId = jwtTokenUtil.getCurrentAdminId();

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 계정을 찾을 수 없습니다."));

        OrganizationSubscriptionResponse response =
                organizationSubscriptionService.getMySubscription(adminId);

        return ResponseEntity.ok(response);
    }
//    @PutMapping("/organization/{organizationId}/{planId}")
//    public SuccessResponse changePlan(
//            @PathVariable Long organizationId,
//            @PathVariable Long planId) {
//        organizationService.changeSubscriptionPlan(organizationId, planId);
//        return new SuccessResponse();
//    }

    /**
     * 기관의 구독상품 변경
     */


    /** ✅ (1) 기관 관리자용: 자기 기관 구독 변경 */
    @PutMapping("/my")
    public ResponseEntity<SuccessResponse> updateMySubscription(
            HttpServletRequest request,
            @RequestBody OrganizationSubscriptionChangeRequest dto
    ) {
        SuccessResponse response = subscriptionService.updateMyOrganizationSubscription(request, dto);
        return ResponseEntity.ok(response);
    }


//    @PutMapping("/{organizationId}/subscription/{subscriptionPlanId}")
//    public DataResponse<OrganizationResponse> changeSubscriptionPlan(
//            @PathVariable Long organizationId,
//            @PathVariable Long subscriptionPlanId
//    ) {
//        OrganizationResponse response = organizationService.changeSubscriptionPlan(organizationId, subscriptionPlanId);
//        return new DataResponse<>(response);
//    }

//    @PutMapping("/organization/{organizationId}/{planId}")
//    public DataResponse<OrganizationResponse> changeSubscriptionPlan(
//            @PathVariable Long organizationId,
//            @PathVariable Long planId
//    ) {
//        OrganizationResponse response = organizationService.changeSubscriptionPlan(organizationId, organizationId);
//        return new DataResponse<>(response);
//    }

//    @GetMapping("/info")
//    public ResponseEntity<?> getSubscriptionInfo(HttpServletRequest request) {
//        return ResponseEntity.ok(subscriptionService.getSubscriptionInfo(request));
//    }
}
