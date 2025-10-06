package com.kaii.dentix.domain.subscriptionPlan.application;

import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscriptionInfo.dto.SubscriptionInfoResponse;
import com.kaii.dentix.domain.subscriptionPlan.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscriptionPlan.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.oral.OralCheckAnalysisState;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.AlreadyDataException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
