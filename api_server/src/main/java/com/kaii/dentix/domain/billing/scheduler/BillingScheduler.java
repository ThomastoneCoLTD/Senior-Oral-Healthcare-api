package com.kaii.dentix.domain.billing.scheduler;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;

import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;

import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingScheduler {

    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;
    private final BillingRepository billingRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * ✅ 매일 자정마다 실행
     * - 만료된 구독(autoRenew=true) 조회
     * - 새 Billing 생성 (UNPAID)
     * - 구독기간 + 기관정보 갱신
     */
    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void generateNextBillingForMonthlySubscriptions() {
        log.info("🕛 [BillingScheduler] 구독 자동 갱신 검사 시작");

        LocalDateTime now = LocalDateTime.now();

        // 1️⃣ 만료된 + 자동갱신 구독 조회
        List<OrganizationSubscription> expiringSubscriptions =
                organizationSubscriptionRepository.findAllBySubscriptionEndDateBeforeAndAutoRenewTrue(now.plusDays(1));

        for (OrganizationSubscription subscription : expiringSubscriptions) {
            Organization organization = subscription.getOrganization();
            SubscriptionPlan plan = subscription.getSubscriptionPlan();

            LocalDateTime newStart = now;
            LocalDateTime newEnd;

            // ✅ planCycle은 String 타입이므로 문자열 비교
            String planCycle = plan.getPlanCycle();
            if ("monthly".equalsIgnoreCase(planCycle)) {
                newEnd = newStart.plusMonths(1);
            } else if ("yearly".equalsIgnoreCase(planCycle)) {
                newEnd = newStart.plusYears(1);
            } else {
                log.warn("⚠️ [{}] 기관의 planCycle 값이 '{}' → 기본 monthly로 처리",
                        organization.getOrganizationName(), planCycle);
                newEnd = newStart.plusMonths(1);
            }

            // ✅ 중복 Billing 방지
            boolean alreadyExists = billingRepository.existsByOrganizationAndPeriodStart(organization, newStart);
            if (alreadyExists) {
                log.info("⚠️ [{}] 기관의 {} 플랜 Billing이 이미 오늘 존재 → 건너뜀",
                        organization.getOrganizationName(),
                        plan.getPlanName());
                continue;
            }

            // ✅ 새 Billing 생성 (결제 미완료 상태)
            Billing newBilling = Billing.builder()
                    .organization(organization)
                    .subscriptionPlan(plan)
                    .billingType(BillingType.SUBSCRIPTION)
                    .billingStatus(BillingStatus.UNPAID)
                    .amount(plan.getPrice())
                    .billedAt(now)
                    .periodStart(newStart)
                    .periodEnd(newEnd)
                    .description(plan.getPlanName().name() + " 구독 자동 갱신")
                    .build();

            billingRepository.save(newBilling);

            // ✅ 구독 정보 갱신
            subscription.setSubscriptionStartDate(newStart);
            subscription.setSubscriptionEndDate(newEnd);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            organizationSubscriptionRepository.save(subscription);

            // ✅ 기관 테이블 구독 정보 동기화
            organization.setSubscriptionStartDate(newStart);
            organization.setSubscriptionEndDate(newEnd);
            organizationRepository.save(organization);

            log.info("✅ [{}] 기관의 {} 플랜 자동 Billing 생성 (UNPAID)",
                    organization.getOrganizationName(),
                    plan.getPlanName());
        }

        log.info("🕛 [BillingScheduler] 구독 자동 갱신 검사 완료");
    }
}