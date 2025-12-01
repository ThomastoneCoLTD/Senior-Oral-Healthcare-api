package com.kaii.dentix.domain.organization.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.oralCheck.domain.OralCheck;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationUsageResponse;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.subscription.application.SubscriptionService;
import com.kaii.dentix.domain.subscription.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Subscription;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrganizationUsageService {
    private final OrganizationRepository organizationRepository;
    private final AdminRepository adminRepository;
    private final OralCheckRepository oralCheckRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionService subscriptionService;
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;

    @Transactional
    public OrganizationUsageResponse getMyOrganizationUsage(Long adminId) {

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundDataException("관리자를 찾을 수 없습니다."));

        Organization organization = admin.getOrganization();
        Long organizationId = organization.getOrganizationId();

        // 🔥 여기! 기존 SubscriptionHistory → OrganizationSubscription 로 교체
        OrganizationSubscription sub = organizationSubscriptionRepository
                .findByOrganization_OrganizationId(organizationId)
                .orElseThrow(() -> new NotFoundDataException("현재 구독 정보가 없습니다."));

        // 사용량 계산
        Long successCount = sub.getSuccessCount().longValue();
        Integer max = sub.getSubscriptionPlan().getMaxSuccessResponses();
        Long remaining = sub.getRemainingResponses().longValue();

        // usageRate는 0~100% → 프론트는 0.0 ~ 1.0을 기대하므로 스케일 변환
        double usageRate = (sub.getUsageRate() != null)
                ? sub.getUsageRate() / 100.0
                : 0.0;

        return OrganizationUsageResponse.builder()
                .subscriptionPlanName(sub.getSubscriptionPlan().getPlanName().name())
                .maxSuccessResponses(max)
                .successCount(successCount)
                .remainingResponses(remaining)
                .usageRate(usageRate)

                .dailyUsage(oralCheckRepository.countTodayUsage(organizationId))
                .weeklyUsage(oralCheckRepository.countThisWeekUsage(organizationId))
                .monthlyUsage(oralCheckRepository.countThisMonthUsage(organizationId))

                .topUsers(oralCheckRepository.findTopUsers(organizationId))
                .recentUsages(oralCheckRepository.findRecentUsages(organizationId))

                .build();
    }
    /**
     * ✅ 성공 응답 기록 및 남은 횟수 반환
     */
//    @Transactional
//    public int recordSuccessAndGetRemaining(Long organizationId) {
//        Organization organization = organizationRepository.findById(organizationId)
//                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 기관입니다."));
//
//        SubscriptionPlan plan = organization.getSubscriptionPlan();
//        int maxQuota = plan.getMaxSuccessResponses();
//
//        // ⚠️ 만료일 경과시 자동 리셋 (예방적)
//        if (organization.getUsageResetDate() != null &&
//                LocalDateTime.now().isAfter(organization.getUsageResetDate())) {
//            organization.resetUsage();
//        }
//
//        // ✅ Organization 내부 비즈니스 로직 활용
//        organization.increaseUsage();
//
//        // ✅ 리포지토리 save → JPA 영속 상태라면 생략 가능
//        organizationRepository.save(organization);
//
//        // ✅ 남은 사용 가능 횟수 계산 (음수면 초과 상태)
//        int remaining = organization.getRemainingResponses();
//        return Math.max(remaining, 0);
//    }
//
//    /**
//     * ✅ 사용량 리셋 (주기 도래 시 자동 리셋)
//     */
//    @Transactional
//    public void resetUsageIfNeeded(Long organizationId) {
//        Organization organization = organizationRepository.findByOrganizationId(organizationId)
//                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 기관입니다."));
//
//        LocalDateTime resetDate = organization.getUsageResetDate();
//        if (resetDate == null) {
//            return;
//        }
//
//        if (LocalDateTime.now().isAfter(resetDate)) {
//            organization.resetUsage();
//            organizationRepository.save(organization);
//        }
//    }

}
