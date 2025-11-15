package com.kaii.dentix.domain.superUser.application;

import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckUsageDto;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationUsageResponse;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.application.SubscriptionService;
import com.kaii.dentix.domain.subscription.dto.SubscriptionHistoryResponse;
import com.kaii.dentix.domain.superUser.dto.OrganizationDetailResponse;
import com.kaii.dentix.domain.superUser.dto.OrganizationListResponse;
import com.kaii.dentix.domain.subscription.dao.SubscriptionHistoryRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import com.kaii.dentix.domain.superUser.dto.SuperAdminBillingDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuperAdminOrganizationService {

    private final OrganizationRepository organizationRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final BillingRepository billingRepository;
    private final OralCheckRepository oralcheckRepository;
    private final SubscriptionService subscriptionService;
    private final OralCheckRepository oralCheckRepository;

    /** ✅ 1. 전체 기관 목록 조회 */
    @Transactional(readOnly = true)
    public List<OrganizationListResponse> getAllOrganizations() {
        return organizationRepository.findAll()
                .stream()
                .map(OrganizationListResponse::fromEntity)
                .toList();
    }
    @Transactional(readOnly = true)
    /** ✅ 2. 기관 상세 정보 조회 */
    public OrganizationDetailResponse getOrganizationDetail(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 기관을 찾을 수 없습니다."));
        return OrganizationDetailResponse.fromEntity(org);
    }
    @Transactional(readOnly = true)
    /** ✅ 3. 기관별 구독이력 조회 */
    public List<SubscriptionHistoryResponse> getOrganizationSubscriptions(Long organizationId) {
        return subscriptionHistoryRepository
                .findAllByOrganization_OrganizationIdOrderByStartDateDesc(organizationId)
                .stream()
                .map(SubscriptionHistoryResponse::fromEntity)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<SuperAdminBillingDto> getOrganizationBillings(Long organizationId) {

        List<Billing> billings = billingRepository.findAllWithOrganizationAndPlan(organizationId);

        return billings.stream()
                .map(SuperAdminBillingDto::new)
                .toList();
    }

    /**
     * 슈퍼관리자 - 특정 기관의 사용자 사용량 조회
     */
    @Transactional
    public OrganizationUsageResponse getOrganizationUsageByOrgId(Long organizationId) {

        // 🔥 기관 존재 여부 확인
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundDataException("기관 정보를 찾을 수 없습니다."));

        // 🔥 현재 구독 정보 조회
        SubscriptionHistory subscriptionHistory =
                subscriptionService.getCurrentSubscription(organizationId);

        // 🔥 성공 카운트 조회
        Long successCount = oralCheckRepository.countSuccessByOrganization(organizationId);

        Integer max = subscriptionHistory.getSubscriptionPlan().getMaxSuccessResponses();
        Long remaining = max - successCount;

        double usageRate = (max == 0) ? 0 : (double) successCount / max;

        return OrganizationUsageResponse.builder()
                .subscriptionPlanName(subscriptionHistory.getSubscriptionPlan().getPlanName().name())
                .maxSuccessResponses(max)
                .successCount(successCount)
                .remainingResponses(remaining)
                .usageRate(usageRate)

                // 🔥 사용량 (일/주/월)
                .dailyUsage(oralCheckRepository.countTodayUsage(organizationId))
                .weeklyUsage(oralCheckRepository.countThisWeekUsage(organizationId))
                .monthlyUsage(oralCheckRepository.countThisMonthUsage(organizationId))

                // 🔥 top users / recent usages
                .topUsers(oralCheckRepository.findTopUsers(organizationId))
                .recentUsages(oralCheckRepository.findRecentUsages(organizationId))

                .build();
    }
}