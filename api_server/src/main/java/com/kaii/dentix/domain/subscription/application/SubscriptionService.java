package com.kaii.dentix.domain.subscription.application;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organization.dto.OrganizationSubscriptionChangeRequest;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.subscription.dao.SubscriptionHistoryRepository;
import com.kaii.dentix.domain.subscription.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscription.dao.SubscriptionUsageRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.domain.type.SubscriptionStatus;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final BillingRepository billingRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;
    private final OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;


    @Transactional
    public SuccessResponse updateMyOrganizationSubscription(
            Admin admin,
            OrganizationSubscriptionChangeRequest dto
    ) {
        Organization organization = admin.getOrganization();

        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(dto.getNewSubscriptionPlanId())
                .orElseThrow(() -> new EntityNotFoundException("구독상품을 찾을 수 없습니다."));

        // =============================
        // 1) Latest OrganizationSubscription 조회
        // =============================
        OrganizationSubscription subscription =
                organizationSubscriptionRepository
                        .findTopByOrganization_OrganizationIdOrderBySubscriptionStartDateDesc(
                                organization.getOrganizationId()
                        )
                        .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();

        // =============================
        // 2) OrganizationSubscription 업데이트
        // =============================
        subscription.setSubscriptionPlan(newPlan);
        subscription.setSubscriptionStartDate(now);
        subscription.setSubscriptionEndDate(now.plusMonths(1));
        subscription.setSubscriptionRenewalDate(now.plusMonths(1));
        subscription.setUsageResetDate(now.plusMonths(1));

        subscription.setSuccessCount(0);
        subscription.setRemainingResponses(newPlan.getMaxSuccessResponses());
        subscription.setUsageRate(0.0);

        subscription.setStatus(SubscriptionStatus.ACTIVE);

        organizationSubscriptionRepository.saveAndFlush(subscription); // UPDATE 발생

        // =============================
        // 3) Organization에도 반영
        // =============================
        organization.setSubscriptionPlan(newPlan);
        organization.setSubscriptionStartDate(now);
        organization.setSubscriptionEndDate(now.plusMonths(1));

        organizationRepository.saveAndFlush(organization);

        // =============================
        // 4) Billing 생성
        // =============================
        Billing billing = new Billing();
        billing.setOrganization(organization);
        billing.setSubscriptionPlan(newPlan);
        billing.setSubscription(subscription);
        billing.setBillingType(BillingType.SUBSCRIPTION);
        billing.setBillingStatus(BillingStatus.PENDING);
        billing.setAmount(newPlan.getPrice());
        billing.setBilledAt(now);
        billing.setPeriodStart(now);
        billing.setPeriodEnd(now.plusMonths(1));
        billing.setDescription("구독상품 변경 결제");

        billingRepository.save(billing);

        // =============================
        // 5) 새로운 OrganizationSubscriptionHistory 저장
        // =============================
        OrganizationSubscriptionHistory history =
                OrganizationSubscriptionHistory.builder()
                        .organization(organization)
                        .subscriptionPlan(newPlan)
                        .status(SubscriptionStatus.ACTIVE)
                        .startDate(now)
                        .endDate(now.plusMonths(1))
                        .reason("구독상품 변경")
                        .build();

        organizationSubscriptionHistoryRepository.save(history);

        return new SuccessResponse(200, "구독상품 변경 완료");
    }
    @Transactional
    public SubscriptionHistory getCurrentSubscription(Long organizationId) {

        List<SubscriptionHistory> list =
                subscriptionHistoryRepository.findAllByOrganization_OrganizationIdOrderByStartDateDesc(organizationId);

        if (list.isEmpty()) {
            throw new NotFoundDataException("구독 이력이 없습니다.");
        }

        return list.get(0);  // 최신 이력 = 현재 구독
    }
}
//    @Transactional
//    public SubscriptionResponse changeSubscriptionPlan(Long orgId, Long newPlanId) {
//        Organization org = organizationRepository.findById(orgId)
//                .orElseThrow(() -> new NotFoundDataException("기관을 찾을 수 없습니다."));
//
//        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(newPlanId)
//                .orElseThrow(() -> new NotFoundDataException("플랜을 찾을 수 없습니다."));
//
//        // ✅ 1. 기존 usage 비활성화
//        subscriptionUsageRepository.deactivateActiveUsage(orgId);
//
//        // ✅ 2. 새 usage 주기 생성
//        LocalDateTime start = LocalDateTime.now();
//        LocalDateTime end = "monthly".equalsIgnoreCase(newPlan.getPlanCycle())
//                ? start.plusMonths(1)
//                : start.plusYears(1);
//
//        SubscriptionResponseDto.SubscriptionCycle usage = SubscriptionResponseDto.SubscriptionCycle.builder()
//                .organization(org)
//                .subscriptionPlan(newPlan)
//                .periodStart(start)
//                .periodEnd(end)
//                .successCount(0)
//                .active(true)
//                .build();
//        subscriptionUsageRepository.save(usage);
//
//        // ✅ 3. Organization 엔티티도 갱신
//        org.setSubscriptionPlan(newPlan);
//        org.setSubscriptionStartDate(start);
//        org.setUsageResetDate(end);
//        organizationRepository.save(org);
//
//        return SubscriptionResponse.builder()
//                .organizationId(org.getOrganizationId())
//                .organizationName(org.getOrganizationName())
//                .subscriptionPlanId(newPlan.getId())
//                .subscriptionPlanName(newPlan.getPlanName())
//                .subscriptionStartDate(start)
//                .usageResetDate(end)
//                .successCount(usage.getSuccessCount())
//                .build();
//    }
//}