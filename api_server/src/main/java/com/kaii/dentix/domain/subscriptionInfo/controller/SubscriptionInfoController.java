package com.kaii.dentix.domain.subscriptionInfo.controller;

import com.kaii.dentix.domain.subscriptionInfo.application.SubscriptionInfoService;
import com.kaii.dentix.domain.subscriptionInfo.application.SubscriptionService;
import com.kaii.dentix.domain.subscriptionInfo.dto.SubscriptionInfoResponse;
import com.kaii.dentix.domain.subscriptionInfo.dto.SubscriptionPlanUpdateRequest;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscription/info")
@RequiredArgsConstructor
public class SubscriptionInfoController {

    private final SubscriptionInfoService subscriptionInfoService;
    private final UserRepository userRepository;
private final SubscriptionService subscriptionPlanService;
    @GetMapping
    public DataResponse<SubscriptionInfoResponse> getSubscriptionInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        Long organizationId = null;

        if (principal instanceof org.springframework.security.core.userdetails.User securityUser) {
            // username이 실제 userId인 경우
            String username = securityUser.getUsername();
            Long userId = Long.parseLong(username);

            // DB에서 유저 조회
            com.kaii.dentix.domain.user.domain.User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            organizationId = user.getOrganization().getOrganizationId();
        }

        if (organizationId == null) {
            throw new RuntimeException("기관 정보를 찾을 수 없습니다.");
        }

        return new DataResponse<>(subscriptionInfoService.getSubscriptionInfo(organizationId));
    }
    @GetMapping("/all")
    public ResponseEntity<List<SubscriptionInfoResponse>> getAllPlans() {
        List<SubscriptionInfoResponse> plans = subscriptionInfoService.getAllPlans();
        return ResponseEntity.ok(plans);
    }
    @GetMapping("/organization/{orgId}")
    public ResponseEntity<SubscriptionInfoResponse> getOrganizationUsage(
            @PathVariable Long orgId) {
        return ResponseEntity.ok(subscriptionInfoService.getSubscriptionInfo(orgId));
    }
    @PutMapping("/{id}")
    public SuccessResponse updatePlan(@PathVariable Long id,
                                      @RequestBody SubscriptionPlanUpdateRequest request) {
        subscriptionPlanService.updatePlan(id, request);
        return new SuccessResponse();
    }
//    @GetMapping("/users")
//    public DataResponse<List<SubscriptionUserUsageResponse>> getSubscriptionUsers(
//            @RequestParam(defaultValue = "ALL") String type
//    ) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        Object principal = authentication.getPrincipal();
//
//        Long organizationId = null;
//
//        if (principal instanceof org.springframework.security.core.userdetails.User securityUser) {
//            String username = securityUser.getUsername();
//            Long userId = Long.parseLong(username);
//
//            com.kaii.dentix.domain.user.domain.User user = userRepository.findByUserId(userId)
//                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
//
//            organizationId = user.getOrganization().getOrganizationId();
//        }
//
//        if (organizationId == null) {
//            throw new RuntimeException("기관 정보를 찾을 수 없습니다.");
//        }
//
//        List<SubscriptionUserUsageResponse> response =
//                subscriptionInfoService.getUserUsageByType(organizationId, type);
//
//        return new DataResponse<>(response);
//    }
}