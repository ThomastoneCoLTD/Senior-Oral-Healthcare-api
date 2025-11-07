package com.kaii.dentix.domain.subscription.application;

import com.kaii.dentix.domain.subscription.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.global.common.error.exception.AlreadyDataException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionPlanService {
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SubscriptionPlanService(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Transactional
    public SubscriptionPlan createPlan(String name, String cycle, Integer sort, Long price, Integer maxSuccessResponses) {
        if(subscriptionPlanRepository.existsByPlanName(name)){
            throw new AlreadyDataException("이미 존재하는 구독 플랜명입니다.");
        }

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .planName(name)
                .planCycle(cycle)
                .planSort(sort)
                .price(price)
                .maxSuccessResponses(maxSuccessResponses)
                .build();
        return subscriptionPlanRepository.save(plan);
    }
}
