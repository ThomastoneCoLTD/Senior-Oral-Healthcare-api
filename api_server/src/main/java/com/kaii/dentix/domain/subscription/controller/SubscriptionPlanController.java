package com.kaii.dentix.domain.subscription.controller;


import com.kaii.dentix.domain.subscription.application.SubscriptionPlanService;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.subscription.dto.SubscriptionPlanResponse;
import com.kaii.dentix.global.common.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/subscriptions")
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    /** ✅ 전체 구독 플랜 조회 */
    @GetMapping("/all")
    public ResponseEntity<DataResponse<List<SubscriptionPlanResponse>>> getAllPlans() {
        List<SubscriptionPlanResponse> plans = subscriptionPlanService.getAllPlans();
        return ResponseEntity.ok(
                new DataResponse<>(200, "전체 구독플랜 조회 성공", plans)
        );
    }
}