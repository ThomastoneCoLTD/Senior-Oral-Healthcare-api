package com.kaii.dentix.domain.subscription.application;

import com.kaii.dentix.domain.subscription.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscription.dto.SubscriptionPlanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor

public class SubscriptionPlanService {
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    /** 전체 구독 플랜 조회 */
    public List<SubscriptionPlanResponse> getAllPlans() {
        return subscriptionPlanRepository.findAllByActiveTrueOrderByPlanSortAsc()
                .stream()
                .map(SubscriptionPlanResponse::from)
                .toList();
    }
}
