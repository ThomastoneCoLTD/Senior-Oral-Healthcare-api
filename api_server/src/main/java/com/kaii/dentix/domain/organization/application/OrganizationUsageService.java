package com.kaii.dentix.domain.organization.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class OrganizationUsageService {
    private final AdminRepository adminRepository;
    private final OralCheckRepository oralCheckRepository;
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;

    @Transactional
    public OrganizationDto.UsageResponse getMyOrganizationUsage(Long adminId) {

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundDataException("관리자를 찾을 수 없습니다."));

        Organization organization = admin.getOrganization();
        Long organizationId = organization.getOrganizationId();

        OrganizationSubscription sub = organizationSubscriptionRepository
                .findByOrganization_OrganizationId(organizationId)
                .orElseThrow(() -> new NotFoundDataException("현재 구독 정보가 없습니다."));

        Integer max = sub.getSubscriptionPlan().getMaxSuccessResponses();

        //LocalDateTime → Date 변환 (Asia/Seoul 기준)
        Date startDate = toDate(sub.getSubscriptionStartDate());
        Date endDate = toDate(resolveUsagePeriodEnd(sub));

        //구독 기간 동안 사용량
        Long successCount = oralCheckRepository.countSubscriptionPeriodUsage(
                organizationId,
                startDate,
                endDate
        );

        //음수 허용 잔여량
        Long remaining = max - successCount;

        double usageRate = (max != null && max > 0)
                ? (double) successCount / max
                : 0.0;

        return OrganizationDto.UsageResponse.builder()
                .subscriptionPlanName(sub.getSubscriptionPlan().getPlanName().name())
                .maxSuccessResponses(max)
                .successCount(successCount)
                .remainingResponses(remaining)
                .usageRate(usageRate)

                .dailyUsage(oralCheckRepository.countTodayUsage(organizationId))
                .weeklyUsage(oralCheckRepository.countThisWeekUsage(organizationId))
                .monthlyUsage(oralCheckRepository.countThisMonthUsage(organizationId))

                // Repository 반환 타입 수정 필요 (아래 Repository 섹션 참조)
                .topUsers(oralCheckRepository.findTopUsers(organizationId))
                .recentUsages(oralCheckRepository.findRecentUsages(organizationId))

                .build();
    }

    private LocalDateTime resolveUsagePeriodEnd(OrganizationSubscription subscription) {
        return subscription.getUsageResetDate() != null
                ? subscription.getUsageResetDate()
                : subscription.getSubscriptionEndDate();
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant());
    }
}
    /**
     * 성공 응답 기록 및 남은 횟수 반환
     */
//    @Transactional
//    public int recordSuccessAndGetRemaining(Long organizationId) {
//        Organization organization = organizationRepository.findById(organizationId)
//                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 기관입니다."));
//
//        SubscriptionPlan plan = organization.getSubscriptionPlan();
//        int maxQuota = plan.getMaxSuccessResponses();
//
//        // 만료일 경과시 자동 리셋 (예방적)
//        if (organization.getUsageResetDate() != null &&
//                LocalDateTime.now().isAfter(organization.getUsageResetDate())) {
//            organization.resetUsage();
//        }
//
//        // Organization 내부 비즈니스 로직 활용
//        organization.increaseUsage();
//
//        // 리포지토리 save → JPA 영속 상태라면 생략 가능
//        organizationRepository.save(organization);
//
//        // 남은 사용 가능 횟수 계산 (음수면 초과 상태)
//        int remaining = organization.getRemainingResponses();
//        return Math.max(remaining, 0);
//    }
//
//    /**
//     * 사용량 리셋 (주기 도래 시 자동 리셋)
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

//}
