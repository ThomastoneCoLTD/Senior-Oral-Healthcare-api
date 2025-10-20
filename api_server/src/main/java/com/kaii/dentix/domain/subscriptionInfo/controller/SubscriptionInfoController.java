package com.kaii.dentix.domain.subscriptionInfo.controller;

import com.kaii.dentix.domain.subscriptionInfo.application.SubscriptionInfoService;
import com.kaii.dentix.domain.subscriptionInfo.dto.SubscriptionInfoResponse;
//import com.kaii.dentix.global.common.response.ApiResponse;
import com.kaii.dentix.domain.subscriptionInfo.dto.SubscriptionUserUsageResponse;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.global.common.response.DataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/subscription/info")
@RequiredArgsConstructor
public class SubscriptionInfoController {

    private final SubscriptionInfoService subscriptionInfoService;
    private final UserRepository userRepository;

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