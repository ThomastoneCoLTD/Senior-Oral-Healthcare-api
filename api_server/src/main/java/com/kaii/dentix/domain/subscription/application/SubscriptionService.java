package com.kaii.dentix.domain.subscription.application;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.dao.SubscriptionUsageRepository;
import com.kaii.dentix.domain.subscription.dto.SubscriptionPlanUpdateRequest;
import com.kaii.dentix.domain.subscription.dto.SubscriptionResponse;
import com.kaii.dentix.domain.subscription.dto.SubscriptionResponseDto;
import com.kaii.dentix.domain.subscription.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionUsageRepository subscriptionUsageRepository;
//    private final SubscriptionUsageRepository subscriptionRepository;
    private final AdminService adminService;
    @Transactional
    public SubscriptionResponse changeSubscriptionPlan(Long orgId, Long newPlanId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundDataException("기관을 찾을 수 없습니다."));

        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(newPlanId)
                .orElseThrow(() -> new NotFoundDataException("플랜을 찾을 수 없습니다."));

        // ✅ 1. 기존 usage 비활성화
        subscriptionUsageRepository.deactivateActiveUsage(orgId);

        // ✅ 2. 새 usage 주기 생성
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = "monthly".equalsIgnoreCase(newPlan.getPlanCycle())
                ? start.plusMonths(1)
                : start.plusYears(1);

        SubscriptionResponseDto.SubscriptionCycle usage = SubscriptionResponseDto.SubscriptionCycle.builder()
                .organization(org)
                .subscriptionPlan(newPlan)
                .periodStart(start)
                .periodEnd(end)
                .successCount(0)
                .active(true)
                .build();
        subscriptionUsageRepository.save(usage);

        // ✅ 3. Organization 엔티티도 갱신
        org.setSubscriptionPlan(newPlan);
        org.setSubscriptionStartDate(start);
        org.setUsageResetDate(end);
        organizationRepository.save(org);

        return SubscriptionResponse.builder()
                .organizationId(org.getOrganizationId())
                .organizationName(org.getOrganizationName())
                .subscriptionPlanId(newPlan.getId())
                .subscriptionPlanName(newPlan.getPlanName())
                .subscriptionStartDate(start)
                .usageResetDate(end)
                .successCount(usage.getSuccessCount())
                .build();
    }
    @Transactional
    public void updatePlan(Long id, SubscriptionPlanUpdateRequest request) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독 상품입니다."));

        if (request.getPlanName() != null) plan.setPlanName(request.getPlanName());
        if (request.getPlanCycle() != null) plan.setPlanCycle(request.getPlanCycle());
        if (request.getPlanSort() != null) plan.setPlanSort(request.getPlanSort());
        if (request.getPrice() != null) plan.setPrice(request.getPrice());
        if (request.getMaxSuccessResponses() != null)
            plan.setMaxSuccessResponses(request.getMaxSuccessResponses());
    }
    @Transactional
    public Object getSubscriptionInfo(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);
        boolean isSuperAdmin = admin.getAdminIsSuper() == YnType.Y;

        if (isSuperAdmin) {
            // ✅ 슈퍼관리자 → 모든 기관 구독정보
            List<SubscriptionResponseDto> all = subscriptionUsageRepository.findAll()
                    .stream()
                    .map(SubscriptionResponseDto::from)
                    .toList();
            return all;
        }

        // ✅ 일반관리자 → 자신의 기관 구독정보 1건
        Long organizationId = admin.getOrganization().getOrganizationId();

        SubscriptionResponseDto.SubscriptionCycle usage = subscriptionUsageRepository
                .findByOrganization_OrganizationIdAndActiveTrue(organizationId)
                .orElseThrow(() -> new NotFoundDataException("활성 구독 정보를 찾을 수 없습니다."));

        return SubscriptionResponseDto.from(usage);
    }
}
