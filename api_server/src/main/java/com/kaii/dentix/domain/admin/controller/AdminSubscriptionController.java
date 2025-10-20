package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.subscriptionInfo.application.SubscriptionInfoService;
import com.kaii.dentix.domain.subscriptionInfo.dto.SubscriptionInfoResponse;
import com.kaii.dentix.domain.user.dao.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final SubscriptionInfoService subscriptionInfoService;
    private final UserRepository userRepository;

    @GetMapping("/all")
    public ResponseEntity<List<SubscriptionInfoResponse>> getAllPlans() {
        List<SubscriptionInfoResponse> plans = subscriptionInfoService.getAllPlans();
        return ResponseEntity.ok(plans);
    }
}
