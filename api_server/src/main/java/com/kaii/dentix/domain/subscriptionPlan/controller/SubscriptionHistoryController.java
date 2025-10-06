package com.kaii.dentix.domain.subscriptionPlan.controller;

import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscriptionPlan.dao.SubscriptionHistoryRepository;
import com.kaii.dentix.domain.subscriptionPlan.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscriptionPlan.dto.SubscriptionHistoryResponse;
import com.kaii.dentix.global.common.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/admin/subscription/history")
@RequiredArgsConstructor
public class SubscriptionHistoryController {

    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * 기관별 구독 이력 조회
     */
    @GetMapping("/{organizationId}")
    public DataResponse<List<SubscriptionHistoryResponse>> getSubscriptionHistory(
            @PathVariable Long organizationId
    ) {
        // 기관 존재 확인
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("기관을 찾을 수 없습니다."));

        // 구독 이력 조회 (최신순)
        List<SubscriptionHistoryResponse> historyList =
                subscriptionHistoryRepository.findAllByOrganization_OrganizationIdOrderByStartDateDesc(organizationId)
                        .stream()
                        .map(history -> SubscriptionHistoryResponse.builder()
                                .planName(history.getSubscriptionPlan().getPlanName())
                                .planCycle(history.getSubscriptionPlan().getPlanCycle())
                                .price(history.getSubscriptionPlan().getPrice())
                                .startDate(history.getStartDate())
                                .endDate(history.getEndDate())
                                .build())
                        .toList();

        return new DataResponse<>(historyList);
    }
}