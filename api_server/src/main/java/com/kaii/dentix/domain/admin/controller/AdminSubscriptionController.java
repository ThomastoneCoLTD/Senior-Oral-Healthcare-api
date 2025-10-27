package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.domain.subscriptionInfo.application.SubscriptionInfoService;
import com.kaii.dentix.domain.subscriptionInfo.application.SubscriptionService;
import com.kaii.dentix.domain.subscriptionInfo.dto.SubscriptionInfoResponse;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
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

    @GetMapping("/all")
    public ResponseEntity<List<SubscriptionInfoResponse>> getAllPlans() {
        List<SubscriptionInfoResponse> plans = subscriptionInfoService.getAllPlans();
        return ResponseEntity.ok(plans);
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
//    @PutMapping("/{organizationId}/subscription/{subscriptionPlanId}")
//    public DataResponse<OrganizationResponse> changeSubscriptionPlan(
//            @PathVariable Long organizationId,
//            @PathVariable Long subscriptionPlanId
//    ) {
//        OrganizationResponse response = organizationService.changeSubscriptionPlan(organizationId, subscriptionPlanId);
//        return new DataResponse<>(response);
//    }

    @PutMapping("/organization/{organizationId}/{planId}")
    public DataResponse<OrganizationResponse> changeSubscriptionPlan(
            @PathVariable Long organizationId,
            @PathVariable Long planId
    ) {
        OrganizationResponse response = organizationService.changeSubscriptionPlan(organizationId, organizationId);
        return new DataResponse<>(response);
    }

    @GetMapping("/info")
    public ResponseEntity<?> getSubscriptionInfo(HttpServletRequest request) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionInfo(request));
    }
}
