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
        LocalDateTime now = LocalDateTime.now();

        // 1️⃣ 기관 조회
        Organization organization = organizationRepository.findById(
                admin.getOrganization().getOrganizationId()
        ).orElseThrow(() -> new EntityNotFoundException("기관을 찾을 수 없습니다."));

        // 2️⃣ 변경할 플랜 조회
        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(
                dto.getNewSubscriptionPlanId()
        ).orElseThrow(() -> new EntityNotFoundException("구독상품을 찾을 수 없습니다."));

        // 3️⃣ 현재 구독 조회 (없으면 생성)
        OrganizationSubscription currentSubscription =
                organizationSubscriptionRepository.findByOrganization(organization)
                        .orElseGet(() -> {
                            OrganizationSubscription s = new OrganizationSubscription();
                            s.setOrganization(organization);
                            return organizationSubscriptionRepository.save(s);
                        });

        // 4️⃣ 기존 구독 → 이력 종료 처리
        if (currentSubscription.getSubscriptionPlan() != null) {

            OrganizationSubscriptionHistory history = OrganizationSubscriptionHistory.builder()
                    .organization(organization)
                    .subscriptionPlan(currentSubscription.getSubscriptionPlan())
                    .startDate(currentSubscription.getSubscriptionStartDate())
                    .endDate(now)
                    .status(SubscriptionStatus.EXPIRED) // ✅ 과거 기록
                    .reason("구독상품 변경")
                    .build();

            organizationSubscriptionHistoryRepository.save(history);
        }


        // 5️⃣ 현재 구독 갱신 (엔티티 책임)
        currentSubscription.initializeSubscription();
        currentSubscription.setSubscriptionPlan(newPlan);

        log.error("SUB ID BEFORE SAVE = {}", currentSubscription.getId());
        log.error("PLAN BEFORE SAVE = {}",
                currentSubscription.getSubscriptionPlan() == null
                        ? "null"
                        : currentSubscription.getSubscriptionPlan().getPlanName()
        );

        organizationSubscriptionRepository.save(currentSubscription);

        log.error("SUB ID AFTER SAVE = {}", currentSubscription.getId());



        // 6️⃣ Billing 생성
        Billing billing = new Billing();
        billing.setOrganization(organization);
        billing.setSubscriptionPlan(newPlan);
        billing.setSubscription(currentSubscription);
        billing.setBillingType(BillingType.SUBSCRIPTION);
        billing.setBillingStatus(BillingStatus.PENDING);
        billing.setAmount(newPlan.getPrice());
        billing.setBilledAt(now);
        billing.setPeriodStart(currentSubscription.getSubscriptionStartDate());
        billing.setPeriodEnd(currentSubscription.getSubscriptionEndDate());
        billing.setDescription("구독상품 변경");

        billingRepository.save(billing);

        return new SuccessResponse(200, "구독상품 변경 완료");
    }



    @Transactional
    public OrganizationSubscription getCurrentSubscription(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundDataException("기관을 찾을 수 없습니다."));

        return organizationSubscriptionRepository.findByOrganization(organization)
                .orElseThrow(() -> new NotFoundDataException("현재 구독 정보가 없습니다."));
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