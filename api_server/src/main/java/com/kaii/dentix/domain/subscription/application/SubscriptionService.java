package com.kaii.dentix.domain.subscription.application;

import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.subscription.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.subscription.dto.SubscriptionDto;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.domain.type.PlanName;
import com.kaii.dentix.domain.type.oral.OralCheckAnalysisState;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.AlreadyDataException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final BillingRepository billingRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;
    private final OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;
    private final OralCheckRepository oralCheckRepository;
    private final UserRepository userRepository;

    // =================================================================
    // 1. 구독 플랜 관리 (Admin)
    // =================================================================

    /** 구독 플랜 생성 */
    public SubscriptionPlan createPlan(String name, String cycle, Integer sort, Long price, Integer maxSuccessResponses, Boolean customSurveyEnabled, Boolean reportExportEnabled, Integer overuseUnitPrice) {
        if (subscriptionPlanRepository.existsByPlanName(name)) {
            throw new AlreadyDataException("이미 존재하는 구독 플랜명입니다.");
        }

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .planName(PlanName.valueOf(name.toUpperCase()))
                .planCycle(cycle)
                .planSort(sort)
                .price(price)
                .maxSuccessResponses(maxSuccessResponses)
                .customSurveyEnabled(customSurveyEnabled)
                .reportExportEnabled(reportExportEnabled)
                .overuseUnitPrice(overuseUnitPrice)
                .build();
        return subscriptionPlanRepository.save(plan);
    }

    /** 전체 구독 플랜 조회 */
    @Transactional(readOnly = true)
    public List<SubscriptionDto.PlanResponse> getAllPlans() {
        // Active 상태인 플랜만 조회
        return subscriptionPlanRepository.findAllByActiveTrueOrderByPlanSortAsc()
                .stream()
                .map(SubscriptionDto.PlanResponse::from)
                .toList();
    }

    /** 구독 플랜 수정 */
    public void updatePlan(Long id, SubscriptionDto.PlanUpdateRequest request) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new NotFoundDataException("구독 플랜을 찾을 수 없습니다."));

        // Entity 업데이트 (Setter 또는 별도 메서드 사용)
        plan.setPlanName(PlanName.valueOf(request.getPlanName().toUpperCase()));
        plan.setPlanCycle(request.getPlanCycle());
        plan.setPlanSort(request.getPlanSort());
        plan.setPrice(request.getPrice());
        plan.setMaxSuccessResponses(request.getMaxSuccessResponses());
    }

    // =================================================================
    // 2. 기관 구독 현황 조회 (Dashboard/Info)
    // =================================================================

    /** 기관 구독 정보 상세 조회 (사용자별 사용량 포함) */
    @Transactional(readOnly = true)
    public SubscriptionDto.InfoResponse getSubscriptionInfo(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundDataException("기관 정보를 찾을 수 없습니다."));

        OrganizationSubscription sub = org.getOrganizationSubscription();
        if (sub == null) {
            throw new NotFoundDataException("구독 정보를 찾을 수 없습니다.");
        }
        SubscriptionPlan plan = sub.getSubscriptionPlan();

        // 1. 사용량 계산
        int totalSuccessCount = sub.getSuccessCount() != null ? sub.getSuccessCount() : 0;
        int max = plan.getMaxSuccessResponses();
        int remaining = Math.max(0, max - totalSuccessCount);
        double usageRate = max == 0 ? 0 : (double) totalSuccessCount / max * 100.0;

        // 2. 사용자별 사용 현황 조회
        List<User> users = userRepository.findByOrganization_OrganizationId(organizationId);
        List<SubscriptionDto.InfoResponse.UserUsage> userUsages = users.stream()
                .map(u -> SubscriptionDto.InfoResponse.UserUsage.builder()
                        .userId(u.getUserId())
                        .userName(u.getUserName())
                        .successCount((int) oralCheckRepository.countByUser_UserIdAndOralCheckAnalysisState(
                                u.getUserId(), OralCheckAnalysisState.SUCCESS))
                        .build())
                .toList();

        return SubscriptionDto.InfoResponse.builder()
                .id(plan.getId())
                .organizationName(org.getOrganizationName())
                .planName(plan.getPlanName().name())
                .planCycle(plan.getPlanCycle())
                .price(plan.getPrice())
                .planSort(plan.getPlanSort())
                .maxSuccessResponses(max)
                .totalSuccessCount(totalSuccessCount)
                .remainingCount(remaining)
                .usageRate(Math.round(usageRate * 10) / 10.0)
                .subscriptionStartDate(sub.getSubscriptionStartDate())
                .usageResetDate(sub.getSubscriptionEndDate())
                .users(userUsages)
                .build();
    }

    /** 특정 기관의 구독 이력 조회 */
    @Transactional(readOnly = true)
    public List<SubscriptionDto.HistoryResponse> getSubscriptionHistory(Long organizationId) {
        return organizationSubscriptionHistoryRepository
                .findAllByOrganization_OrganizationIdOrderByStartDateDesc(organizationId)
                .stream()
                .map(SubscriptionDto.HistoryResponse::fromEntity)
                .toList();
    }

    // =================================================================
    // 3. 구독 변경 및 갱신 로직
    // =================================================================

    /** 내 기관 구독 변경 요청 */
    public SuccessResponse updateMyOrganizationSubscription(
            Admin admin,
            OrganizationDto.SubscriptionChangeRequest dto // 기관 DTO 사용
    ) {
        LocalDateTime now = LocalDateTime.now();

        Organization organization = organizationRepository.findById(
                admin.getOrganization().getOrganizationId()
        ).orElseThrow(() -> new EntityNotFoundException("기관을 찾을 수 없습니다."));

        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(
                dto.getNewSubscriptionPlanId()
        ).orElseThrow(() -> new EntityNotFoundException("구독상품을 찾을 수 없습니다."));

        // 1. 기존 활성 구독 이력 종료
        organizationSubscriptionHistoryRepository
                .findByOrganization_OrganizationIdAndEndDateIsNull(organization.getOrganizationId())
                .ifPresent(active -> {
                    if (active.getSubscriptionPlan().getId().equals(newPlan.getId())) {
                        throw new IllegalStateException("이미 적용된 구독입니다.");
                    }
                    active.expire(now);
                });

        // 2. 새 구독 이력 생성
        OrganizationSubscriptionHistory newHistory = OrganizationSubscriptionHistory.builder()
                .organization(organization)
                .subscriptionPlan(newPlan)
                .startDate(now)
                .endDate(null) // 활성 상태
                .reason("구독상품 변경")
                .successCount(0)
                .remainingResponses(
                        newPlan.getMaxSuccessResponses() != null ? newPlan.getMaxSuccessResponses() : 0
                )
                .build();

        organizationSubscriptionHistoryRepository.save(newHistory);

        // 3. Billing(결제) 데이터 생성
        Billing billing = new Billing();
        billing.setOrganization(organization);
        billing.setSubscriptionPlan(newPlan);
        billing.setBillingType(BillingType.SUBSCRIPTION);
        billing.setBillingStatus(BillingStatus.PENDING);
        billing.setAmount(newPlan.getPrice());
        billing.setBilledAt(now);
        billing.setPeriodStart(now);
        billing.setDescription("구독상품 변경");

        billingRepository.save(billing);

        return new SuccessResponse(200, "구독상품 변경 완료");
    }

    /** 현재 구독 정보 Entity 조회 (내부용) */
    @Transactional(readOnly = true)
    public OrganizationSubscription getCurrentSubscription(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundDataException("기관을 찾을 수 없습니다."));

        return organizationSubscriptionRepository.findByOrganization(organization)
                .orElseThrow(() -> new NotFoundDataException("현재 구독 정보가 없습니다."));
    }
}