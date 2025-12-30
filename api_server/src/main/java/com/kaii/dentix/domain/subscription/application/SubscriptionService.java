package com.kaii.dentix.domain.subscription.application;

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
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.domain.type.BillingStatus;

import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final BillingRepository billingRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;
    private final OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;

    @Transactional
    public SuccessResponse updateMyOrganizationSubscription(
            Admin admin,
            OrganizationSubscriptionChangeRequest dto
    ) {
        LocalDateTime now = LocalDateTime.now();

        //기관 조회
        Organization organization = organizationRepository.findById(
                admin.getOrganization().getOrganizationId()
        ).orElseThrow(() -> new EntityNotFoundException("기관을 찾을 수 없습니다."));

        //변경할 플랜 조회
        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(
                dto.getNewSubscriptionPlanId()
        ).orElseThrow(() -> new EntityNotFoundException("구독상품을 찾을 수 없습니다."));

        //기존 활성 구독 이력 종료
        organizationSubscriptionHistoryRepository
                .findByOrganization_OrganizationIdAndEndDateIsNull(
                        organization.getOrganizationId()
                )
                .ifPresent(active -> {
                    if (active.getSubscriptionPlan().getId().equals(newPlan.getId())) {
                        throw new IllegalStateException("이미 적용된 구독입니다.");
                    }
                    active.expire(now);
                });

        //새 구독 이력 생성
        OrganizationSubscriptionHistory newHistory =
                OrganizationSubscriptionHistory.builder()
                        .organization(organization)
                        .subscriptionPlan(newPlan)
                        .startDate(now)
                        .endDate(null) // 활성
                        .reason("구독상품 변경")
                        .successCount(0)
                        .remainingResponses(newPlan.getMaxSuccessResponses())
                        .build();

        organizationSubscriptionHistoryRepository.save(newHistory);

        //Billing 생성 (history 기준)
        Billing billing = new Billing();
        billing.setOrganization(organization);
        billing.setSubscriptionPlan(newPlan);
        billing.setBillingType(BillingType.SUBSCRIPTION);
        billing.setBillingStatus(BillingStatus.PENDING);
        billing.setAmount(newPlan.getPrice());
        billing.setBilledAt(now);
        billing.setPeriodStart(now);
        billing.setPeriodEnd(null);
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