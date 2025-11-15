package com.kaii.dentix.domain.subscription.application;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.dto.OrganizationSubscriptionChangeRequest;
import com.kaii.dentix.domain.subscription.dao.SubscriptionHistoryRepository;
import com.kaii.dentix.domain.subscription.dao.SubscriptionUsageRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import com.kaii.dentix.domain.subscription.dto.SubscriptionPlanUpdateRequest;
import com.kaii.dentix.domain.subscription.dto.SubscriptionResponse;
import com.kaii.dentix.domain.subscription.dto.SubscriptionResponseDto;
import com.kaii.dentix.domain.subscription.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.domain.type.PlanName;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionHistoryService subscriptionHistoryService;
    private final BillingRepository billingRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionUsageRepository subscriptionUsageRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    //    private final SubscriptionUsageRepository subscriptionRepository;
    private final AdminService adminService;

    @Transactional
    public SuccessResponse updateMyOrganizationSubscription(
            HttpServletRequest request,
            OrganizationSubscriptionChangeRequest dto
    ) {
        // ✅ 1. 기관 조회
        Organization organization = organizationRepository.findById(dto.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("기관을 찾을 수 없습니다."));

        // ✅ 2. 새 구독상품 조회
        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(dto.getNewSubscriptionPlanId())
                .orElseThrow(() -> new EntityNotFoundException("구독상품을 찾을 수 없습니다."));

        // ✅ 3. 기관 정보 먼저 업데이트 + flush (detach 방지)
        organization.setSubscriptionPlan(newPlan);
        organization.setSubscriptionStartDate(LocalDateTime.now());
        organization.setSubscriptionEndDate(LocalDateTime.now().plusMonths(1));
        organizationRepository.saveAndFlush(organization);

        // ✅ 4. 구독이력 기록 (기존 종료 + 신규 추가)
        subscriptionHistoryService.recordPlanChange(organization, newPlan, "구독상품 변경");

        // ✅ 5. Billing 생성
        Billing billing = new Billing();
        billing.setOrganization(organization);
        billing.setSubscriptionPlan(newPlan);
        billing.setBillingType(BillingType.SUBSCRIPTION);
        billing.setBillingStatus(BillingStatus.PENDING);
        billing.setAmount(newPlan.getPrice());
        billing.setPeriodStart(LocalDateTime.now());
        billing.setPeriodEnd(organization.getSubscriptionEndDate());
        billing.setDescription("구독상품 변경 결제");
        billingRepository.save(billing);

        return new SuccessResponse(200, "기관 구독상품 변경 완료");
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