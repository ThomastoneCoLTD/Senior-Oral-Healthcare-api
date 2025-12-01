package com.kaii.dentix.domain.superUser.application;

import com.kaii.dentix.domain.admin.dao.user.AdminUserCustomRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.superAdmin.SuperAdminUserStatisticResponse;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.billing.dto.BillingListResponse;
import com.kaii.dentix.domain.billing.dto.BillingOveruseResponse;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckUsageDto;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationUsageResponse;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.subscription.application.SubscriptionService;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.subscription.dto.SubscriptionHistoryResponse;
import com.kaii.dentix.domain.superUser.dto.*;
import com.kaii.dentix.domain.subscription.dao.SubscriptionHistoryRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
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
    private final UserRepository userRepository;
    private final AdminUserCustomRepository adminUserCustomRepository;
    private final OrganizationRepository subscriptionRepository;
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

    @Transactional(readOnly = true)
    public SuperAdminAllUserStatisticsResponse getSuperAdminTotalUserStatistics(Admin admin) {

        // 🔒 슈퍼관리자 권한 체크
        if (admin.getAdminIsSuper() != YnType.Y) {
            throw new BadRequestApiException("슈퍼관리자만 접근할 수 있습니다.");
        }

        // 📌 기관별 사용자 통계 (기존 커스텀 쿼리)
        List<SuperAdminUserStatisticResponse> orgStats =
                adminUserCustomRepository.getAllOrganizationUserStats();

        // 📌 전체 사용자 수
        long totalUsers = userRepository.count();

        long maleUsers = userRepository.countByUserGender(GenderType.M);
        long femaleUsers = userRepository.countByUserGender(GenderType.W);

        // 📌 최근 7일 신규 가입자
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        Date oneWeekAgoDate = java.sql.Timestamp.valueOf(oneWeekAgo);

        long newUsers = userRepository.countByCreatedAfter(oneWeekAgoDate);

        return SuperAdminAllUserStatisticsResponse.builder()
                .totalUsers(totalUsers)
                .maleUsers(maleUsers)
                .femaleUsers(femaleUsers)
                .newUsers7Days(newUsers)
                .organizationStats(orgStats)
                .build();
    }

    public SuperAdminCurrentSubscriptionDto getCurrentSubscription(Long orgId) {

        Organization org = organizationRepository.findByOrganizationId(orgId)
                .orElseThrow(() -> new NotFoundDataException("기관을 찾을 수 없습니다."));

        if (org.getOrganizationSubscription() == null) {
            throw new NotFoundDataException("현재 구독 상품이 없습니다.");
        }

        return SuperAdminCurrentSubscriptionDto.builder()
                .organizationId(org.getOrganizationId())
                .organizationName(org.getOrganizationName())
                .subscriptionPlanId(org.getSubscriptionPlan().getId())
                .subscriptionPlanName(org.getSubscriptionPlan().getPlanName().name())
                .startDate(org.getOrganizationSubscription().getSubscriptionStartDate())
                .endDate(org.getOrganizationSubscription().getSubscriptionEndDate())
                .build();
    }
    public SuperAdminBillingListResponse getOrganizationBillingForSuperAdmin(Long organizationId) {

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("기관 없음"));

        List<Billing> billings = billingRepository
                .findByOrganizationAndBillingTypeInOrderByBilledAtDesc(
                        org,
                        List.of(
                                BillingType.MONTHLY,
                                BillingType.SUBSCRIPTION,
                                BillingType.REGULAR
                        )
                );

        return SuperAdminBillingListResponse.from(org, billings);
    }
    public BillingOveruseResponse getOveruseDetail(Long billingId) {

        Billing baseBilling = billingRepository.findById(billingId)
                .orElseThrow(() -> new EntityNotFoundException("Billing 없음"));

        if (!(baseBilling.getBillingType() == BillingType.SUBSCRIPTION ||
                baseBilling.getBillingType() == BillingType.REGULAR ||
                baseBilling.getBillingType() == BillingType.MONTHLY)) {
            throw new IllegalArgumentException("구독 관련 Billing만 조회 가능합니다.");
        }

        List<Billing> overuseList = billingRepository.findByOrganizationAndBillingTypeAndBilledAtBetween(
                baseBilling.getOrganization(),
                BillingType.OVERUSE,
                baseBilling.getPeriodStart(),
                baseBilling.getPeriodEnd()
        );

        Long totalAmount = overuseList.stream().mapToLong(Billing::getAmount).sum();

        return BillingOveruseResponse.builder()
                .billingId(baseBilling.getId())
                .planName(baseBilling.getSubscriptionPlan().getPlanName().name())
                .periodStart(baseBilling.getPeriodStart())
                .periodEnd(baseBilling.getPeriodEnd())
                .baseAmount(baseBilling.getAmount())
                .totalOveruseAmount(totalAmount)
                .totalOveruseCount((long) overuseList.size())
                .overuseList(
                        overuseList.stream()
                                .map(BillingOveruseResponse.Item::from)
                                .toList()
                )
                .build();
    }

}