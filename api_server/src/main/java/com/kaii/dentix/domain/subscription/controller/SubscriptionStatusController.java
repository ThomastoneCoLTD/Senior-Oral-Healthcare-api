package com.kaii.dentix.domain.subscription.controller;

import com.kaii.dentix.domain.subscription.application.SubscriptionStatusService;
import com.kaii.dentix.domain.subscription.dto.SubscriptionStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ✅ 구독 상태 조회 컨트롤러
 * 기관의 구독상품 + 사용현황 + 월별 사용량 + 초과 여부 조회
 */
@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class SubscriptionStatusController {

    private final SubscriptionStatusService subscriptionStatusService;

    /**
     * ✅ GET /api/subscription/{orgId}/status
     */
    @GetMapping("/{orgId}/status")
    public ResponseEntity<SubscriptionStatusResponse> getSubscriptionStatus(@PathVariable Long orgId) {
        SubscriptionStatusResponse response = subscriptionStatusService.getSubscriptionStatus(orgId);
        return ResponseEntity.ok(response);
    }
}