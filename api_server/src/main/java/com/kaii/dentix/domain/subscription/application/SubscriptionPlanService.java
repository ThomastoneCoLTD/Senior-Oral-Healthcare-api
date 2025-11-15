package com.kaii.dentix.domain.subscription.application;

import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.subscription.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.subscription.dto.SubscriptionPlanResponse;
import com.kaii.dentix.domain.type.PlanName;
import com.kaii.dentix.global.common.error.exception.AlreadyDataException;
import jakarta.persistence.Column;
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
    public SubscriptionPlan createPlan(String name, String cycle, Integer sort, Double price, Integer maxSuccessResponses, Boolean customSurveyEnabled, Boolean reportExportEnabled, Integer overuseUnitPrice) {
        if(subscriptionPlanRepository.existsByPlanName(name)){
            throw new AlreadyDataException("이미 존재하는 구독 플랜명입니다.");
        }

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .planName(PlanName.valueOf(name.toUpperCase()))
                .planCycle(cycle)
                .planSort(sort)
                .price(price)
                .maxSuccessResponses(maxSuccessResponses)
                .customSurveyEnabled(customSurveyEnabled)
                .reportExportEnabled(reportExportEnabled)
                .overuseUnitPrice(overuseUnitPrice)
                .build();
        return subscriptionPlanRepository.save(plan);
    }

    /** 전체 구독 플랜 조회 */
    public List<SubscriptionPlanResponse> getAllPlans() {
        return subscriptionPlanRepository.findAllByActiveTrueOrderByPlanSortAsc()
                .stream()
                .map(SubscriptionPlanResponse::from)
                .toList();
    }
}
