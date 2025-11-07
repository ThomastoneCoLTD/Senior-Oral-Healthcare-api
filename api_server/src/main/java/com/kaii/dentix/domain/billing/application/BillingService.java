package com.kaii.dentix.domain.billing.application;

import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.*;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.BillingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingService {
    private final BillingRepository billingRepository;

    /** ✅ 월 구독요금 Billing 생성 */
    @Transactional
    public void createMonthlyBilling(Organization org) {
        SubscriptionPlan plan = org.getSubscriptionPlan();

        Billing billing = Billing.builder()
                .organization(org)
                .amount(plan.getPrice())
                .description(plan.getName() + " 월 구독 요금")
                .status(BillingStatus.UNPAID)
                .createdAt(LocalDateTime.now())
                .build();

        billingRepository.save(billing);
    }

    /** ✅ 초과요금 Billing 생성 */
    @Transactional
    public void createOveruseBilling(Organization org, int overuseCount) {
        int unit = org.getSubscriptionPlan().getOveruseUnitPrice();
        int total = overuseCount * unit;

        if (total <= 0) return;

        Billing billing = Billing.builder()
                .organization(org)
                .amount(total)
                .description("AI 분석 초과 사용 (" + overuseCount + "회)")
                .status(BillingStatus.UNPAID)
                .createdAt(LocalDateTime.now())
                .build();

        billingRepository.save(billing);
    }

    /** ✅ 미정산 내역 합계 조회 */
    public int getUnpaidTotal(Long orgId) {
        return billingRepository.sumUnpaidAmount(orgId);
    }

    /** ✅ 월말 일괄 정산 (결제는 아니지만 상태 갱신) */
    @Transactional
    public void settleMonthlyBilling(Long orgId) {
        List<Billing> bills = billingRepository.findByOrganizationIdAndStatus(orgId, BillingStatus.UNPAID);
        for (Billing b : bills) b.setStatus(BillingStatus.PAID);
    }
}