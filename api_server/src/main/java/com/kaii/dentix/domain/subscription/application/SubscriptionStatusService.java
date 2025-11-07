package com.kaii.dentix.domain.subscription.application;

import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.dao.SubscriptionUsageHistoryRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.subscription.domain.SubscriptionUsageHistory;
import com.kaii.dentix.domain.subscription.dto.SubscriptionStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class SubscriptionStatusService {

    private final OrganizationRepository organizationRepository;
    private final SubscriptionUsageHistoryRepository usageRepository;

    /**
     * ✅ 기관별 구독상품 + 사용현황 + 월별 사용량 조회
     */
    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getSubscriptionStatus(Long orgId) {
        Organization organization = organizationRepository.findWithSubscriptionPlanById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("기관을 찾을 수 없습니다."));

        SubscriptionPlan plan = organization.getSubscriptionPlan();

        // 기본 값
        int successCount = organization.getSuccessCount() != null ? organization.getSuccessCount() : 0;
        int remaining = organization.getRemainingResponses() != null ? organization.getRemainingResponses() : 0;
        double usageRate = (plan.getMaxSuccessResponses() > 0)
                ? (double) successCount / plan.getMaxSuccessResponses() * 100 : 0.0;
        boolean overused = remaining <= 0;

        // ✅ 이번 달 사용 이력 조회
        SubscriptionUsageHistory usage = usageRepository
                .findByOrganizationIdAndUsageMonth(orgId, YearMonth.now())
                .orElse(null);

        int monthUsedCount = (usage != null) ? usage.getUsedCount() : 0;
        int overusedCount = (usage != null) ? usage.getOverusedCount() : 0;

        return SubscriptionStatusResponse.builder()
                .organizationId(organization.getOrganizationId())
                .organizationName(organization.getOrganizationName())
                .planName(plan.getPlanName())
                .planCycle(plan.getPlanCycle())
                .planPrice(plan.getPrice())
                .planQuota(plan.getMaxSuccessResponses())
                .usedCount(successCount)
                .remainingCount(remaining)
                .usageRate(usageRate)
                .overused(overused)
                .monthUsedCount(monthUsedCount)
                .overusedCount(overusedCount)
                .build();
    }
}