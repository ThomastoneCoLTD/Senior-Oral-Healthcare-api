package com.kaii.dentix.domain.billing.scheduler;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.billing.application.BillingService;
//import com.kaii.dentix.domain.subscription.dao.SubscriptionUsageHistoryRepository;
import com.kaii.dentix.domain.subscription.dao.SubscriptionUsageRepository;
//import com.kaii.dentix.domain.subscription.domain.SubscriptionUsageHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MonthlyBillingScheduler {
    private final OrganizationRepository organizationRepository;
    private final BillingService billingService;
//    private final SubscriptionUsageHistoryRepository usageRepository;

//    @Scheduled(cron = "0 0 0 1 * ?") // 매월 1일 00시
//    @Transactional
//    public void handleMonthlyBilling() {
//        List<Organization> orgs = organizationRepository.findAll();
//
//        for (Organization org : orgs) {
//            // 1️⃣ 초과 요금 Billing 생성
//            SubscriptionUsageHistory usage = usageRepository.findByOrganizationIdAndUsageMonth(
//                    org.getOrganizationId(), YearMonth.now().minusMonths(1)
//            ).orElse(null);
//
//            if (usage != null && usage.getOverusedCount() > 0) {
//                billingService.createOveruseBilling(org, usage.getOverusedCount());
//            }
//
//            // 2️⃣ 기본 구독요금 Billing 생성
//            billingService.createMonthlyBilling(org);
//
//            // 3️⃣ 사용량 리셋
//            org.resetUsage();
//        }
//    }
}